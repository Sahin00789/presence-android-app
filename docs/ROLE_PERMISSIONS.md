# Role Permission Matrix

## Roles Overview
- **Owner**: Head of the organization (`organization_id`) who manages everything across the organization and all assigned premises: self-registers, creates organization & configures all premises, directly adds Employees across premises, views organization-wide reports & workforce, approves financial adjustments, and locks monthly payroll (`monthly_payrolls`). **The Owner is NOT an employee.**
- **Employee**: Premises-level workforce directly added by the Owner (`premises_id`). Authenticates via Google or email OTP, performs face + GPS clock-in/out, undergoes periodic active-shift GPS checks, tracks shift schedules and attendance. **Employees ARE employees (`role = 'EMPLOYEE'`) who earn salary through payroll.**

## Permission Matrix

| Feature / Action | Owner (Head of Organization) | Employee (Workforce Member) |
| :--- | :--- | :--- |
| **Organization & Premises Configuration** | Yes | No |
| **Employee Creation & Premises Assignment** | Yes | No |
| **Shift Definitions & Assignments** | Yes | View Assigned Only |
| **Face Enrollment (`multi_pose_face_templates`)**| Yes (All Employees) | No (Self-service verify only) |
| **Attendance Exceptions & GPS Alerts** | Review & Resolve | Submit Late Request |
| **Leave Approval** | Approve / Reject | Request Leave |
| **Overtime Approval & Booking** | Create & Approve | View Assigned Only |
| **Payroll Processing & Locking** | Calculate & Lock | View Payslips |
| **Earning Ledger Access** | Organization-wide | Personal Ledger |
