# Attendance State Machine & Policy Engine (`docs/ATTENDANCE_RULES.md`)

## 1. Attendance State Transition Diagram

```mermaid
stateDiagram-v2
    [*] --> SCHEDULED : Shift Assigned
    SCHEDULED --> CLOCKED_IN : Geofence + Face Match
    SCHEDULED --> ABSENT : No Clock-In & Shift Ended
    SCHEDULED --> ON_LEAVE : Approved Leave Request
    SCHEDULED --> HOLIDAY : Organization Holiday
    SCHEDULED --> WEEKLY_OFF : Scheduled Off Day

    CLOCKED_IN --> PRESENT : Timely Clock-In
    CLOCKED_IN --> LATE : Clocked-In After Grace Period
    CLOCKED_IN --> HALF_DAY : Worked < Half-Day Threshold
    CLOCKED_IN --> EXCEPTION_OPEN : 3 Failed GPS Checks / Geofence Breach

    EXCEPTION_OPEN --> CORRECTED_PRESENT : Manager Resolves (Present)
    EXCEPTION_OPEN --> CORRECTED_ABSENT : Manager Resolves (Absent)
    EXCEPTION_OPEN --> DISPUTED : Staff Contests Manager Decision

    PRESENT --> CLOCKED_OUT : Shift Completed
    LATE --> CLOCKED_OUT : Shift Completed
    HALF_DAY --> CLOCKED_OUT : Shift Completed

    HOLIDAY --> PRESENT : Clocked-In on Holiday (normal present day)
    WEEKLY_OFF --> PRESENT : Clocked-In on Off-Day (normal present day)
```

---

## 2. Clock-In Validation Policy
1. **Distance Check**: Distance between device GPS coordinates and premise coordinates must be $\le 50\text{ metres}$.
2. **Accuracy Threshold**: GPS reading `accuracyMeters` must be $\le 30\text{ metres}$. Stale location readings ($>2\text{ minutes}$ old) or flagged mock locations are rejected.
3. **Face Verification Check**: Cosine similarity score between clock-in embedding and enrolled template must exceed the configured threshold ($\ge 0.75$).
4. **Shift Match**: Clock-in is validated against assigned shift schedules.

---

## 3. Late Arrival & Half-Day Rules
1. **Grace Period**: Default 15 minutes after shift `start_time`. Clock-in within grace period is marked `PRESENT`.
2. **Late Threshold**: Clock-in past grace period is marked `LATE`.
3. **Half-Day Threshold**: Total worked duration $< 50\%$ of scheduled shift minutes is marked `HALF_DAY`.
4. **Early Departure**: Clock-out prior to scheduled shift end time minus grace period is flagged as Early Departure.

---

## 4. Manual Resolution & Exception Workflows
1. **Immutable History**: Original clock-in event rows are never deleted or modified.
2. **Correction Audit**: Manual overrides create an `attendance_corrections` entry referencing the original `attendance_exceptions` record, storing `actor_id`, timestamp, and reason text.
3. **Approval Escalation**: Manager corrections involving financial rate changes or sensitive absences require Owner review if configured by policy.
