# Role & Permissions Matrix (`docs/ROLE_PERMISSIONS.md`)

## 1. Scope & Authority
- **Owner**: Head of the organization (`organization_id`) who manages everything across the organization and all its premises. Possesses exclusive administration rights for organization creation, direct addition of Managers with mandatory premises assignment (`premises_id`), global premises configuration, payroll locking (`monthly_payrolls`), sensitive financial approvals, and supervised face enrollment of Managers and Staff (`multi_pose_face_templates`). **The Owner is NOT an employee** (represented by `organizations.owner_user_id`), does not reside in the `employees` table, has **NO attendance tracking**, and does **NOT earn an employee salary**.
- **Manager**: Premises-level head (`premises_id`), NOT organization-level. Directly added by the Owner with mandatory premises assignment. Manages daily operations, directly adds Staff to their assigned premises, configures and assigns premises-level shifts (`shifts`, `employee_shift_assignments`), performs face enrollment for Staff within their premises (`multi_pose_face_templates`), reviews attendance exceptions and GPS monitoring pings, and handles leave/corrections. **Managers ARE employees** (`employees` table with `role = 'MANAGER'`) — they have their own assigned shifts, track attendance (`employee_attendance`), and earn salary through monthly payroll (`monthly_payrolls`) and the earning ledger (`staff_earning_ledger`).
- **Staff**: Premises-level workforce directly added by their premises Manager. **Staff ARE employees** (`employees` table with `role = 'STAFF'`) scoped to their Manager's `premises_id`. They perform geofenced face clock-in/clock-out, participate in periodic GPS monitoring checks (`attendance_location_pings`), view shift schedules, track attendance, and earn salary through monthly payroll and the staff earning ledger (`staff_earning_ledger`). Staff cannot self-enroll biometrics.

---

## 2. Permissions Table

| Feature / Action | Owner (Head of Organization) | Manager (Head of Assigned Premises) | Staff (Employee Self) |
| :--- | :---: | :---: | :---: |
| **Is Employee?** | **No** (Head of Org) | **Yes** (Premises Employee) | **Yes** (Premises Employee) |
| **Has Attendance & Salary Tracking?** | **No** (Exempt) | **Yes** (Tracked & Earns Salary) | **Yes** (Tracked & Earns Salary) |
| **Organization Management** | Full Control | Read Only | No |
| **Premises Configuration** | All Premises (Create/Edit) | Assigned Premises Only (Read) | Assigned Premises Only (Read) |
| **Manager Creation & Premises Assignment** | Yes | No | No |
| **Staff Creation & Premises Assignment** | Yes (All Premises) | Yes (Assigned Premises Only) | No |
| **Shift Definition & Assignment** | Yes (All Premises) | Yes (Assigned Premises Only) | View Assigned Shifts |
| **Clock-In / Attendance Participation** | **No** (Owner has no attendance) | **Yes** (Own attendance tracked) | **Yes** (Own attendance tracked) |
| **Attendance Review & Corrections** | All Premises | Assigned Premises Only | Own Records Only |
| **Payroll Review & Calculation** | All Employees | Assigned Premises Staff (+ Own) | Own Payslips Only |
| **Payroll Locking (`monthly_payrolls`)** | Exclusive Right | No | No |
| **Salary Disbursal (`payroll_transactions`)** | Yes | No | No |
| **Face Enrollment (`multi_pose_face_templates`)**| Yes (Managers & Staff) | Yes (Assigned Premises Staff) | Supervised Only (No Self-Enrollment) |
| **Earnings & Payslips Tracking** | All Employees (Ledger Oversight) | Assigned Premises (+ Own Salary) | Own Salary & Payslips Only |

