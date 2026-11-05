# Functional & Non-Functional Requirements

- **FR-AUTH-1**: Support Email 6-Digit OTP and Google OAuth 2.0 authentication for all roles (`OWNER`, `EMPLOYEE`).
- **FR-EMP-1**: Manage employees through the `employees` table with multi-tenant organization scoping (`organization_id`) and premises assignment (`premises_id`).
- **FR-ATT-1**: Perform face + GPS clock-in/out verification for employees against assigned premises geofence.
- **FR-MON-1**: Execute active-shift GPS monitoring every ~10 minutes, generating alerts after 3 consecutive failures.
- **FR-PAY-1**: Maintain append-only staff earning ledger (`staff_earning_ledger`) and calculate monthly payroll (`monthly_payrolls`).
