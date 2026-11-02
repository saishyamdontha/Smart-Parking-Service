"""One full parking session end to end. Tolerates a dead activity-service (a fault we test)."""
import sys
import time

import requests

PARKING, SESSION, ACTIVITY = "http://localhost:8081", "http://localhost:8082", "http://localhost:8083"


def count_events():
    """Number of recorded events, or None if activity-service cannot be reached."""
    try:
        return len(requests.get(f"{ACTIVITY}/api/events", timeout=3).json())
    except requests.RequestException:
        return None


def main():
    spaces = requests.get(f"{PARKING}/api/spaces", timeout=5).json()
    free = [s for s in spaces if str(s.get("status", "")).upper() not in ("OCCUPIED", "RESERVED")]
    if not free:
        sys.exit("No free space found")
    space_id = free[0]["id"]

    before = count_events()
    r = requests.post(f"{SESSION}/api/sessions", json={"userId": "smoke-user", "spaceId": space_id}, timeout=10)
    print("start session:", r.status_code, r.text[:200])
    if not r.ok:
        sys.exit("session could not be started")
    session_id = r.json()["id"]

    time.sleep(1.5)  # activity events are recorded asynchronously
    after = count_events()
    print(f"activity events: {before} -> {after}")

    r = requests.post(f"{SESSION}/api/sessions/{session_id}/end", timeout=10)
    print("end session:", r.status_code, r.text[:200])

    if before is None or after is None:
        print("NOTE: activity-service unreachable; the session result above is what a user would see")
    elif after > before:
        print("SMOKE TEST OK")
    else:
        print("WARNING: no activity event recorded")


if __name__ == "__main__":
    main()
