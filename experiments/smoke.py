"""One full parking session end to end: list spaces, start a session, check the event, end it."""
import sys
import time

import requests

PARKING, SESSION, ACTIVITY = "http://localhost:8081", "http://localhost:8082", "http://localhost:8083"


def main():
    spaces = requests.get(f"{PARKING}/api/spaces", timeout=5).json()
    print(f"{len(spaces)} spaces; first one: {spaces[0] if spaces else None}")
    free = [s for s in spaces if str(s.get("status", "")).upper() not in ("OCCUPIED", "RESERVED")]
    if not free:
        sys.exit("No free space found - check the space statuses printed above")
    space_id = free[0]["id"]

    events_before = len(requests.get(f"{ACTIVITY}/api/events", timeout=5).json())
    r = requests.post(f"{SESSION}/api/sessions", json={"userId": "smoke-user", "spaceId": space_id}, timeout=10)
    print("start session:", r.status_code, r.text[:200])
    r.raise_for_status()
    session_id = r.json()["id"]

    time.sleep(1.5)  # activity events are recorded asynchronously
    events_after = len(requests.get(f"{ACTIVITY}/api/events", timeout=5).json())
    print(f"activity events: {events_before} -> {events_after}")

    r = requests.post(f"{SESSION}/api/sessions/{session_id}/end", timeout=10)
    print("end session:", r.status_code, r.text[:200])
    r.raise_for_status()
    print("SMOKE TEST OK" if events_after > events_before else "WARNING: no activity event recorded")


if __name__ == "__main__":
    main()
