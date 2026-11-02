# Fault-localisation results

3 repetitions per scenario, 6 sessions per run. Localised = the level named exactly the true faulty component.

| Scenario | True location | L0 localised | L1 localised | L2 localised |
|---|---|---|---|---|
| S1 parking-service down | parking-service | 0/3 | 3/3 | 3/3 |
| S2 activity-service down (silent) | activity-service | 0/3 | 3/3 | 3/3 |
| S3 parking database down | parking-db | 0/3 | 3/3 | 3/3 |
| S4 session database down | session-db | 0/3 | 3/3 | 3/3 |
| S5 parking-service slow, above timeout (3 s) | parking-service | 0/3 | 0/3 | 3/3 |
| S6 parking-service intermittent errors (50%) | parking-service | 0/3 | 0/3 | 3/3 |
| S7 activity-service rejects events (silent) | activity-service | 0/3 | 0/3 | 3/3 |
| S8 session-service misconfigured (wrong parking URL) | session-service | 0/3 | 0/3 | 3/3 |
| H1 activity database down (held out) | activity-db | 0/3 | 3/3 | 3/3 |
| H2 parking-service slow, below timeout (1.5 s) (held out) | parking-service | 0/3 | 0/3 | 0/3 |

**Designed scenarios (S1-S8):** L0: 0/24 localised, 18/24 detected; L1: 12/24 localised, 21/24 detected; L2: 24/24 localised, 24/24 detected

**Held-out scenarios (H1-H2):** L0: 0/6 localised, 0/6 detected; L1: 3/6 localised, 3/6 detected; L2: 3/6 localised, 3/6 detected
