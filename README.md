# Smart Parking Service: Diagnostics and Fault Localisation

A three-service Spring Boot system (parking spaces, sessions and activity events, each with its own PostgreSQL
database), extended with service-level diagnostics and an automated fault-injection harness to answer one question:

> **How well can each level of diagnostic information locate a fault, and which faults stay hidden?**

> **Note:** the system was built for the course *Applied Cloud Computing and Big Data* at BTH. The diagnostics,
> fault injection and evaluation were added **after the course was completed (November 2026), with AI assistance**.

## Architecture
frontend ──► session-service (8082) ──┬──► parking-service (8081) ──► parking-db
│ └──► activity-service (8083) ──► activity-db
└──► session-db
- `session-service` is the hub: it occupies a space in `parking-service` (must succeed) and records an event in
  `activity-service` (asynchronously; a failure does not fail the session).
- Kubernetes manifests from the course are in `k8s/`. For experiments, the whole system runs with Docker Compose.

## What was added
| Area | What |
|---|---|
| **Correlation IDs** | Every request gets an ID, passed to the other services and written in every log line, including asynchronous calls |
| **Access log** | One line per request: method, path, status and duration, tagged with the correlation ID |
| **Coded errors** | Structured codes such as `SES-PARKING-TIMEOUT` or `SES-ACTIVITY-UNREACHABLE` instead of one generic 502 |
| **Timeouts** | Outgoing calls fail after 2 seconds instead of hanging |
| **`/diagnostics` endpoint** | Per service: error counts by code, the last 50 errors with correlation IDs, and dependency status |
| **Fault injection** | `/admin/faults` adds latency or internal errors to parking or activity service (experimenter only) |
| **Fail-fast database checks** | 1-second connection timeout, so a database outage is reported instead of hanging health checks |

## The experiment
`experiments/evaluate.py` injects each fault, runs the same workload (6 parking sessions), and localises the fault at
three diagnostic levels:

| Level | What the diagnoser may see |
|---|---|
| **L0** | Only the HTTP status codes users received |
| **L1** | L0 + each service's health endpoint (diagnostic **access**) |
| **L2** | L1 + `/diagnostics` error codes and dependency views (diagnostic **information**) |

**Scenarios:** S1 parking-service down, S2 activity-service down (silent), S3 parking database down, S4 session
database down, S5 parking-service slow above the timeout, S6 intermittent errors (50%), S7 activity-service rejects
events (silent), S8 wrong parking URL in session-service. **Held out:** H1 activity database down, H2 parking-service
slow *below* the timeout.

## Results (3 runs per scenario)
| Level | Designed faults localised (S1–S8) | Held-out faults localised (H1–H2) |
|---|---|---|
| L0: status codes only | 0/24 (6/24 not even detected) | 0/6 |
| L1: + health checks | 12/24 | 3/6 |
| L2: + service-level diagnostics | 24/24 | 3/6 |

Full table: [`results/summary.md`](results/summary.md).

## Findings
1. **Status codes alone never located a fault**, and silent failures (S2, S7) were not even noticed: users saw
   success while events were lost.
2. **Health checks caught crashes and database outages** but not slowdowns, intermittent errors, silent failures or
   misconfiguration. Only the richer diagnostic information localised those.
3. **A database outage made healthy services look dead.** Before the fail-fast fix, health checks hung for up to
   28 seconds waiting for a connection, so the harness blamed the service instead of its database. With a 1-second
   connection timeout, every database fault was localised.
4. **Timeouts left orphaned state:** session-service gave up after 2 seconds, but parking-service still completed
   the request later, leaving spaces occupied with no session.
5. **Remaining gap:** a slowdown *below* the timeout (H2) went undetected at every level, because no service tracks
   latency as a diagnostic signal.

## Limitations
- The L1/L2 localisation rules were written knowing the designed scenarios, so 24/24 on S1–S8 is partly by design.
  The held-out scenarios (3/6) are the fairer test.
- Faults are injected in a controlled setting with a small workload; real systems have more noise.
- The tag `before-failfast` marks the code before the fail-fast fix.

## How to run
```bash
docker compose up -d --build                 # all three services and databases
python3 -m venv experiments/.venv && source experiments/.venv/bin/activate
pip install requests
python experiments/smoke.py                  # one full session end to end
python -u experiments/evaluate.py            # the full experiment (about 20 minutes)
cat results/summary.md
```
