# System Requirements (`docs/REQUIREMENTS.md`)

## 1. Functional Requirements
- **FR-AUTH-1**: Support Email 6-Digit OTP and Google OAuth 2.0 authentication for all roles (`OWNER`, `MANAGER`, `STAFF`).
- **FR-AUTH-2**: Enforce single-device session concurrency via `logged_devices`.
- **FR-EMP-1**: Manage staff and managers through the `employees` table with multi-tenant organization isolation.
- **FR-SHIFT-1**: Configure shifts (`shifts`) and assign shifts per employee (`employee_shift_assignments`).
- **FR-ATT-1**: Clock-in requiring premises geofence validation and local ONNX multi-pose face verification (`multi_pose_face_templates`).
- **FR-ATT-2**: Periodic ~10-minute GPS location pings (`attendance_location_pings`).
- **FR-PAY-1**: Record earnings in append-only `staff_earning_ledger` and process monthly payrolls (`monthly_payrolls`, `payroll_transactions`).

## 2. Non-Functional Requirements
- **NFR-SEC-1**: Secure cryptographic storage and JWT token generation with Argon2/bcrypt password/OTP hashing.
- **NFR-PERF-1**: Sub-50ms local face verification inference via XNNPACK.
- **NFR-PERF-2**: PostgreSQL transaction isolation for financial and attendance records.
