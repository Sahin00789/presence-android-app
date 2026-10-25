# Geofencing & Periodic GPS Monitoring Engine (`docs/LOCATION_MONITORING.md`)

## 1. Premises Geofencing Specification
- **Geofence Radius**: Configurable per premises; default **50 metres**.
- **Distance Formula**: Spherical Geodesic Haversine formula executed on Android & backend:
  $$d = 2r \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)}\right)$$
- **Max Location Accuracy Allowed**: $30\text{ metres}$. If $Accuracy > 30\text{m}$, the location reading is marked `LOW_ACCURACY` and re-attempted.

---

## 2. Periodic GPS Checks (~10-Minute Active Shift Engine)

During active shift hours, the Android client executes periodic background location checks.

```mermaid
sequenceDiagram
    autonumber
    participant Work as WorkManager / Foreground Service
    participant Fused as Fused Location Provider
    participant Local as Room Sync DB
    participant API as Fastify API
    participant DB as PostgreSQL

    Work->>Fused: Request High-Accuracy Location Ping (~10 min interval)
    Fused-->>Work: Location Reading (Lat, Long, Accuracy, MockFlag, Timestamp)
    alt Reading Valid & In Premises (<= 50m)
        Work->>API: POST /api/v1/location/checks (Status: IN_PREMISES)
        API->>DB: Record location_check_events (Success)
        API-->>Work: Check Recorded & Reset Failure Counter
    else Reading Out of Premises or Stale / Mocked
        Work->>API: POST /api/v1/location/checks (Status: OUTSIDE / MOCKED)
        API->>DB: Record location_check_events (Failure)
        API->>API: Increment Consecutive Failure Counter for Active Session
        alt Consecutive Failures == 3
            API->>DB: Create location_monitoring_exception record
            API->>API: Send Real-Time Notification to Manager & Staff
        end
    end
```

---

## 3. The 3-Consecutive-Failures Rule & Recovery

### Policy Specification
1. **Trigger**: When an active monitoring session records **3 consecutive** failed location checks (Outside premises, Mocked location, Stale reading, or Location disabled).
2. **Alert Broadcast**:
   - High-priority push / SSE notification sent to Manager dashboard.
   - Alert notification sent to Staff device explaining location check failure.
3. **No Automatic Absence**:
   - **CRITICAL**: The 3-failure rule does **NOT** automatically mark the employee absent or penalize salary.
   - It creates an open `location_monitoring_exception` item in the Manager's review queue.
4. **Counter Reset**:
   - A single valid `IN_PREMISES` check resets the consecutive failure counter to 0.
   - Manager manual resolution closes the open exception.
5. **Deduplication**: Repeated failed pings after the 3rd failure update the existing open exception rather than spamming duplicate alert notifications.
