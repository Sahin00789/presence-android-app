# Approval Workflows (`docs/APPROVAL_WORKFLOWS.md`)

## 1. Overview
Operational and financial workflows in `presence` require explicit authorization and approval before updating authoritative state in PostgreSQL (`employees`, `employee_attendance`, `leave_requests`, `monthly_payrolls`, `staff_earning_ledger`).

---

## 2. Leave Request Approval Workflow
- **Submission**: Staff or Manager submits a leave request (`leave_requests`).
- **Notification**: Manager and Owner receive real-time alerts.
- **Review**: Manager or Owner approves or rejects the leave request. Upon approval, attendance status is automatically updated to `LEAVE` for the assigned shift.

---

## 3. Attendance Exception & Manual Resolution Workflow (3 Consecutive Outside Pings)
- **Exception Generation**: Triggered by face mismatch, geofence breach, or **3 consecutive outside GPS monitoring pings** (`outside count > 3`).
- **Action Buttons Persistence**: Action buttons appear on the Owner and Manager dashboard and remain active **until the shift or overtime is completely finished/completed**.
- **Manual Marking Options**: Owner or Manager can manually mark attendance as `ABSENT`, `HALF_DAY`, `LEAVE`, or resume tracking (`RESUME`).
- **Default Present Policy**: If the owner or manager **does not press any button** before the shift/overtime completes, attendance **defaults to Present** (`PRESENT`).
- **Audit Logging**: All manual overrides are tracked in `audit_logs` and `attendance_corrections`.

---

## 4. Payroll Approval & Disbursal Workflow
- **Monthly Calculation**: Background engine calculates shift earnings, holiday pay, and overtime.
- **Owner Review & Lock**: Owner approves and locks the monthly payroll period (`monthly_payrolls`).
- **Disbursal**: Payments are recorded via `payroll_transactions` updating the payroll status to `PAID`.
