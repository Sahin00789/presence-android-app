# Salary Calculation Engine & Compensation Models (`docs/SALARY_RULES.md`)

## 1. Shift Payment & Attendance Rules (`shift_payment_rules` / `AssignShiftWithRulesModal`)

Configured per staff member or manager for shift templates and attendance statuses. All payment and penalty amounts for each shift status are explicitly entered by the Owner or Manager through `AssignShiftWithRulesModal` or salary rule dialogs, and stored in integer minor units (paise/cents), not automatically generated:
- **PRESENT**: Explicit shift payment rate entered by Owner/Manager (e.g. ₹600.00 / shift).
- **LATE**: Explicit shift payment rate minus explicit late penalty (`penalty_cents`) entered by Owner/Manager.
- **HALF_DAY**: Explicit half-day shift payment rate entered by Owner/Manager (e.g. ₹300.00 / shift).
- **ABSENT**: Zero base pay plus optional explicit absent penalty entered by Owner/Manager.

Assigning shift schedules via `AssignShiftWithRulesModal` allows setting independent shift templates and status rates for each day of the week (Sun–Sat) alongside global unworked day rules.

---

## 2. Unworked & Special Day Rules (`unworked_payment_rules`)

Configures holiday, weekly off-day, and paid-leave compensation explicitly entered by Owner or Manager:
1. **Unworked Paid Holidays (`HOLIDAY`)**: Explicit daily payment rate entered by Owner/Manager for scheduled public holidays.
2. **Weekly Off Days (`WEEKLY_OFF`)**: Explicit daily payment rate entered by Owner/Manager for scheduled off-days that are not worked.
3. **Paid Leave (`PAID_LEAVE`)**: Explicit daily payment rate for approved paid/sick leave.

There are no separate worked-holiday or worked-off-day rules. Clock-ins on holidays or weekly offs are paid as normal present days.

Rules are effective-dated and resolved against the payroll period start date. A staff-specific rule overrides the organization default for the same day type. The engine emits itemized earnings as `HOLIDAY_PAY`, `PAID_LEAVE`, and `WEEKLY_OFF_PAY`.

---

## 3. Overtime Tracking & Consolidation (`overtime`)

Overtime is recorded in the `overtime` table with explicit attributes:
- `work_date`, `start_time`, `end_time`
- `planned_minutes`, `worked_minutes`
- `payment_cents` (agreed payment amount entered by Owner/Manager)
- `status` (`REQUESTED`, `APPROVED`, `REJECTED`, `COMPLETED`, `PAID`, `CLOSED`)

During monthly salary calculation, all completed and approved overtime entries (`status` IN `APPROVED`, `COMPLETED`, `PAID`, `CLOSED`) for the month are aggregated directly into the employee's monthly earnings.

---

## 4. Financial Precision & Integer Minor Units
1. **Zero Floating-Point Math**: All financial rates, additions, deductions, and gross/net calculations run on integer minor units (`bigint` paise/cents).
2. **Rounding Policy**: Half-up rounding applied at final daily/monthly summary stages.
3. **Effective-Dated Rates**: Salary and payment rule modifications create historical snapshots with an `effective_from` date. Retroactive monthly calculations preserve historical rule snapshots.
