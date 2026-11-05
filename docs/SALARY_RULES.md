# Salary Calculation Engine & Compensation Models (`docs/SALARY_RULES.md`)

## 1. Shift Payment & Attendance Rules (`shift_payment_rules` / `AssignShiftWithRulesModal`)
> [!NOTE]
> Salary calculation and compensation models apply exclusively to **Employees**. The **Owner** is the head of the organization, is NOT an employee, and does NOT have attendance or an employee salary.

Configured per employee for shift templates and attendance statuses. All payment and penalty amounts for each shift status are explicitly entered by the Owner through `AssignShiftWithRulesModal` or salary rule dialogs, and stored in whole integer Indian Rupees (INR / ₹), not automatically generated:
- **PRESENT**: Explicit shift payment rate entered by Owner (e.g. ₹600 / shift).
- **LATE**: Explicit shift payment rate minus explicit late penalty entered by Owner.
- **HALF_DAY**: Explicit half-day shift payment rate entered by Owner (e.g. ₹300 / shift).
- **ABSENT**: Zero base pay plus optional explicit absent penalty entered by Owner.

Assigning shift schedules via `AssignShiftWithRulesModal` allows setting independent shift templates and status rates for each day of the week (Sun–Sat) alongside global unworked day rules.

---

## 2. Unworked & Special Day Rules (`unworked_payment_rules`)

Configures holiday, weekly off-day, and paid-leave compensation explicitly entered by Owner:
1. **Unworked Paid Holidays (`HOLIDAY`)**: Explicit daily payment rate entered by Owner for scheduled public holidays.
2. **Weekly Off Days (`WEEKLY_OFF`)**: Explicit daily payment rate entered by Owner for scheduled off-days that are not worked.
3. **Paid Leave (`PAID_LEAVE`)**: Explicit daily payment rate for approved paid/sick leave.

There are no separate worked-holiday or worked-off-day rules. Clock-ins on holidays or weekly offs are paid as normal present days.

Rules are resolved by precedence (employee-specific > premises-specific > organization default). The engine emits itemized earnings as `HOLIDAY_PAY`, `PAID_LEAVE`, and `WEEKLY_OFF_PAY`.

---

## 3. Overtime Tracking & Consolidation (`overtime`)

Overtime is recorded in the `overtime` table with explicit attributes:
- `work_date`, `start_time`, `end_time`
- `planned_minutes`, `worked_minutes`
- `payment_inr` (agreed payment amount in INR entered by Owner)
- `status` (`REQUESTED`, `APPROVED`, `REJECTED`, `COMPLETED`, `PAID`, `CLOSED`)

During monthly salary calculation, all completed and approved overtime entries (`status` IN `APPROVED`, `COMPLETED`, `PAID`, `CLOSED`) for the month are aggregated directly into the employee's monthly earnings.

---

## 4. Financial Precision & Integer Indian Rupees (INR)
1. **Zero Floating-Point Math**: All financial rates, additions, and gross/net calculations run on whole integer Indian Rupees (INR / ₹). No paise, no cents, no decimal currency fractions.
2. **Rounding Policy**: Whole integer rounding applied at summary stages.
3. **Effective-Dated Rates**: Salary and payment rule modifications create historical snapshots with an `effective_from` date. Retroactive monthly calculations preserve historical rule snapshots.
