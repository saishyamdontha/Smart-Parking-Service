"""Fault-localisation experiment.

For each scenario: inject a fault, run a fixed workload, collect what is observable at three
diagnostic levels, and localise the fault at each level with fixed, pre-written rules.
  L0: HTTP status codes seen by users                (like ECU fault codes)
  L1: L0 + each service's health endpoint            (diagnostic access)
  L2: L1 + /diagnostics error codes and dependencies (richer diagnostic information)
The diagnosis side never reads /admin/faults.
"""
import json
import subprocess
import time
from pathlib import Path

import requests

P, S, A = "http://localhost:8081", "http://localhost:8082", "http://localhost:8083"
SERVICES = {"parking-service": P, "session-service": S, "activity-service": A}
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "results"
REPEATS = 3
WORKLOAD_SESSIONS = 6


# ---------- infrastructure helpers ----------
def compose(*args):
    subprocess.run(["docker", "compose", *args], cwd=ROOT, check=True, capture_output=True)


def wait_healthy(timeout=120):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            if all(requests.get(u + "/actuator/health", timeout=2).status_code == 200 for u in SERVICES.values()):
                return
        except requests.RequestException:
            pass
        time.sleep(2)
    raise RuntimeError("system did not become healthy")


def set_fault(url, **body):
    requests.post(url + "/admin/faults", json=body, timeout=5).raise_for_status()


def clear_faults():
    for u in (P, A):
        try:
            requests.delete(u + "/admin/faults", timeout=5)
        except requests.RequestException:
            pass


def reset_state(timeout=120):
    """End open sessions, release all spaces (including orphans left by timeouts), reset diagnostics.
    Retries until every service answers, because a service may still be starting after recovery."""
    deadline = time.time() + timeout
    while True:
        try:
            for s in requests.get(S + "/api/sessions", timeout=5).json():
                if s.get("status") == "ACTIVE":
                    requests.post(f"{S}/api/sessions/{s['id']}/end", timeout=10)
            for sp in requests.get(P + "/api/spaces", timeout=5).json():
                if sp.get("status") != "AVAILABLE":
                    requests.post(f"{P}/api/spaces/{sp['id']}/release", timeout=5)
            for u in SERVICES.values():
                requests.post(u + "/diagnostics/reset", timeout=5).raise_for_status()
            return
        except (requests.RequestException, ValueError):
            if time.time() > deadline:
                raise RuntimeError("could not reset state: a service is not answering")
            time.sleep(3)


# ---------- scenarios: (id, description, true location, inject, recover, held_out) ----------
def stop(svc):
    return lambda: compose("stop", svc)


def start(svc):
    return lambda: compose("start", svc)


def bad_url():
    compose("-f", "docker-compose.yml", "-f", "experiments/override-badurl.yml", "up", "-d", "session-service")
    wait_healthy()


def good_url():
    compose("up", "-d", "session-service")


SCENARIOS = [
    ("S1", "parking-service down", "parking-service", stop("parking-service"), start("parking-service"), False),
    ("S2", "activity-service down (silent)", "activity-service", stop("activity-service"), start("activity-service"), False),
    ("S3", "parking database down", "parking-db", stop("parking-db"), start("parking-db"), False),
    ("S4", "session database down", "session-db", stop("session-db"), start("session-db"), False),
    ("S5", "parking-service slow, above timeout (3 s)", "parking-service",
     lambda: set_fault(P, mode="LATENCY", latencyMs=3000, rate=1.0), clear_faults, False),
    ("S6", "parking-service intermittent errors (50%)", "parking-service",
     lambda: set_fault(P, mode="ERROR", rate=0.5), clear_faults, False),
    ("S7", "activity-service rejects events (silent)", "activity-service",
     lambda: set_fault(A, mode="ERROR", rate=1.0), clear_faults, False),
    ("S8", "session-service misconfigured (wrong parking URL)", "session-service", bad_url, good_url, False),
    ("H1", "activity database down (held out)", "activity-db", stop("activity-db"), start("activity-db"), True),
    ("H2", "parking-service slow, below timeout (1.5 s) (held out)", "parking-service",
     lambda: set_fault(P, mode="LATENCY", latencyMs=1500, rate=1.0), clear_faults, True),
]


# ---------- workload and observations ----------
def workload():
    """Start and end WORKLOAD_SESSIONS sessions. Returns only the HTTP status codes users saw."""
    statuses = []
    for i in range(WORKLOAD_SESSIONS):
        try:
            r = requests.post(S + "/api/sessions", json={"userId": f"eval-{i}", "spaceId": i + 1}, timeout=10)
            statuses.append(r.status_code)
            if r.status_code == 201:
                e = requests.post(f"{S}/api/sessions/{r.json()['id']}/end", timeout=10)
                statuses.append(e.status_code)
        except requests.RequestException:
            statuses.append("NO_RESPONSE")
    return statuses


def health_view():
    view = {}
    for name, u in SERVICES.items():
        try:
            j = requests.get(u + "/actuator/health", timeout=3).json()
            view[name] = {"status": j.get("status"), "db": j.get("components", {}).get("db", {}).get("status")}
        except requests.Timeout:
            view[name] = {"status": "UNRESPONSIVE", "db": None}   # up, but not answering in time
        except (requests.RequestException, ValueError):
            view[name] = {"status": "UNREACHABLE", "db": None}    # connection refused: process is down
    return view


def diagnostics_view():
    view = {}
    for name, u in SERVICES.items():
        try:
            view[name] = requests.get(u + "/diagnostics", timeout=5).json()
        except (requests.RequestException, ValueError):
            view[name] = None
    return view


# ---------- localisation rules (fixed before the experiment) ----------
CALL_PATH = ["session-service", "session-db", "parking-service", "parking-db"]


def localise_l0(statuses):
    errors = [s for s in statuses if s not in (200, 201, 204)]
    return list(CALL_PATH) if errors else []


def localise_l1(statuses, health):
    found = []
    for svc, h in health.items():
        db = svc.replace("-service", "-db")
        if h["status"] == "UNREACHABLE":
            found.append(svc)
        elif h["status"] == "UNRESPONSIVE":
            found += [svc, db]          # stuck: the service or something it waits on
        elif h["db"] == "DOWN":
            found.append(db)
    return list(dict.fromkeys(found)) or localise_l0(statuses)


def db_down(diag):
    return diag is not None and str(diag.get("dependencies", {}).get("database", "")).startswith("DOWN")


def localise_l2(statuses, health, diag):
    """Read the diagnostic error codes and dependency views first; fall back to L1 only if they say nothing."""
    sess = diag.get("session-service") or {}
    park = diag.get("parking-service")
    act = diag.get("activity-service")
    codes = set(sess.get("errorCounts", {}))
    found = []
    if "SES-PARKING-TIMEOUT" in codes or "SES-PARKING-HTTP_5XX" in codes:
        found.append("parking-db" if db_down(park) else "parking-service")
    if "SES-PARKING-UNREACHABLE" in codes:
        # Reachable from outside but not from session-service: session-service's view of it is wrong.
        found.append("session-service" if health["parking-service"]["status"] == "UP" else "parking-service")
    if any(c.startswith("SES-ACTIVITY-") for c in codes):
        found.append("activity-service" if act is None else ("activity-db" if db_down(act) else "activity-service"))
    if "SES-INTERNAL" in codes:
        found.append("session-db" if db_down(sess) else "session-service")
    if sess == {} and health["session-service"]["status"] in ("UNREACHABLE", "UNRESPONSIVE"):
        found += localise_l1(statuses, health)
    found = list(dict.fromkeys(found))
    return found or localise_l1(statuses, health)


# ---------- run ----------
def score(candidates, truth):
    return {"detected": bool(candidates), "localised": candidates == [truth], "candidates": candidates}


def run():
    wait_healthy()
    clear_faults()
    reset_state()
    records = []
    for rep in range(1, REPEATS + 1):
        for sid, desc, truth, inject, recover, held_out in SCENARIOS:
            print(f"[rep {rep}] {sid}: {desc}", flush=True)
            inject()
            time.sleep(2)
            statuses = workload()
            time.sleep(2)  # let async calls finish
            health, diag = health_view(), diagnostics_view()
            rec = {
                "rep": rep, "scenario": sid, "description": desc, "truth": truth, "held_out": held_out,
                "statuses": statuses, "health": health,
                "session_codes": (diag.get("session-service") or {}).get("errorCounts"),
                "L0": score(localise_l0(statuses), truth),
                "L1": score(localise_l1(statuses, health), truth),
                "L2": score(localise_l2(statuses, health, diag), truth),
            }
            records.append(rec)
            print(f"   L0={rec['L0']['candidates']}  L1={rec['L1']['candidates']}  L2={rec['L2']['candidates']}",
                  flush=True)
            recover()
            wait_healthy()
            time.sleep(3)
            reset_state()
    OUT.mkdir(exist_ok=True)
    (OUT / "raw.json").write_text(json.dumps(records, indent=2, default=str))
    write_summary(records)


def write_summary(records):
    lines = ["# Fault-localisation results", "",
             f"{REPEATS} repetitions per scenario, {WORKLOAD_SESSIONS} sessions per run. "
             "Localised = the level named exactly the true faulty component.", "",
             "| Scenario | True location | L0 localised | L1 localised | L2 localised |",
             "|---|---|---|---|---|"]
    for sid, desc, truth, *_ in SCENARIOS:
        rs = [r for r in records if r["scenario"] == sid]
        cells = [f"{sum(r[l]['localised'] for r in rs)}/{len(rs)}" for l in ("L0", "L1", "L2")]
        lines.append(f"| {sid} {desc} | {truth} | " + " | ".join(cells) + " |")
    for group, flag in (("Designed scenarios (S1-S8)", False), ("Held-out scenarios (H1-H2)", True)):
        rs = [r for r in records if r["held_out"] == flag]
        parts = [f"{l}: {sum(r[l]['localised'] for r in rs)}/{len(rs)} localised, "
                 f"{sum(r[l]['detected'] for r in rs)}/{len(rs)} detected" for l in ("L0", "L1", "L2")]
        lines += ["", f"**{group}:** " + "; ".join(parts)]
    (OUT / "summary.md").write_text("\n".join(lines) + "\n")
    print((OUT / "summary.md").read_text())


if __name__ == "__main__":
    run()
