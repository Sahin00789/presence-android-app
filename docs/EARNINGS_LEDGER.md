# Append-Only Employee Earning Ledger Architecture (`docs/EARNINGS_LEDGER.md`)

## 1. Append-Only Earning Ledger Principle
> [!NOTE]
> The earning ledger applies exclusively to employees (**Staff** and **Managers** residing in the `employees` table). The **Owner** is the head of the organization, is NOT an employee, has NO attendance tracking, and does NOT have an earning ledger or salary.

Employee earnings (covering both staff and managers via the `employees` table) are recorded in an append-only ledger table `staff_earning_ledger`. Direct mutable balance updates and legacy transaction tables are not used. Every earning record is tied to specific work events (shift attendance, holidays, off-days, overtime).

---

## 2. Earning Ledger Types & Accounting Rules

| Earning Type (`earning_type`) | Source Table Reference | Trigger Event |
| :--- | :--- | :--- |
| `SHIFT` | `employee_attendance` / `employee_shift_assignments` | Verified daily shift attendance completion (posted when shift attendance is completed/finalized) |
| `HOLIDAY` | `holiday_calendars` | Automated paid holiday earnings credit (via jobs service for days marked as holiday in `holiday_calendars`; no shift attendance required) |
| `OFF_DAY` | `weekly_offs` | Automated weekly-off earnings credit (via jobs service for days marked as weekly off in `weekly_offs` and with no shifts assigned for that weekday; no shift attendance required) |
| `OVERTIME` | `overtime` | Completed and approved overtime hours (posted when overtime is completed/approved and finalized) |

Notes:
- Holiday and weekly off earnings are posted independently by the jobs service. They are not dependent on attendance/overtime.
- A single work date can have multiple ledger rows per employee (e.g. SHIFT + OVERTIME, or independent entries).
- Shift assignments define working days; weekly offs are derived from weekdays with no active shift assignment.

---

## 3. Monthly Payroll Settlement Pipeline

```mermaid
sequenceDiagram
    autonumber
    participant Shift as Shift Attendance Finalization
    participant OT as Overtime Completion
    participant Jobs as Jobs Service (src/jobs/holidayWeeklyOffEarnings.ts)
    participant API as Fastify API
    participant DB as PostgreSQL (employee_earning_ledger)
    participant Payroll as Monthly Payroll Engine (`monthly_payrolls`)
    participant Android as Android App UI

    Shift->>API: Daily Attendance (SHIFT) Completed/Finalized
    API->>DB: INSERT INTO employee_earning_ledger (type='SHIFT', amount, employee_id, earning_date, attendance_id)
    OT->>API: Overtime (COMPLETED/APPROVED) Finalized
    API->>DB: INSERT INTO employee_earning_ledger (type='OVERTIME', amount, employee_id, earning_date, overtime_id)
    Jobs->>DB: For dates in holiday_calendars -> INSERT employee_earning_ledger (type HOLIDAY); For dates matching weekly_offs (no shift assigned) -> INSERT employee_earning_ledger (type OFF_DAY). Independent of attendance/overtime. Idempotent.
    API->>DB: Update monthly_payrolls accumulated earned amounts
    API->>Android: SSE / WebSocket Broadcast (Ledger & Payroll Update)
    Android->>Android: Refresh Employee Earnings & Monthly Salary Roster
```

---

## 4. Automated Daily Earnings for Holidays & Weekly Offs (Jobs Service)
- **Location**: `server/src/jobs/holidayWeeklyOffEarnings.ts` (started via `startHolidayWeeklyOffEarningsJob`).
- **When**: Runs on a schedule (started on app listen) to process holidays and weekly offs.
- **Logic**:
  - **Holidays**: For each date present in `holiday_calendars`, post `HOLIDAY` entry to `employee_earning_ledger` for eligible employees as per rules. This is independent of shift attendance/overtime.
  - **Weekly Offs**: For each date that is a weekly off per `weekly_offs` (days with no active shift assigned for the employee/weekday), post `OFF_DAY` entry. This is independent of shift attendance/overtime.
- **Amount**: Resolved from `unworked_payment_rules` (effective-dated) for the employee/premises/organization.
- **Idempotency**: Prevented via `source_reference`/unique constraints and checks for existing ledger entries for that date/type/employee. The job is safe to rerun.
- **Multiple rows per day**: Allowed - a single day can have multiple ledger entries (e.g. SHIFT and OVERTIME, or multiple independent postings).

## 5. Financial Safeguards
1. **Source Reference Uniqueness**: Unique constraints on `employee_earning_ledger` (attendance/overtime + type) prevent duplicate earning postings.
2. **PostgreSQL Transactions**: Ledger insertions execute inside strict SQL transactions.
3. **Auditability**: Every earning entry maintains exact foreign key references to `employees`, `premises`, `shift_assignments`, `attendance_id`, or `overtime_id`.
