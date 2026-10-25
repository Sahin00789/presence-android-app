# Monthly Payroll & Salary Calculation Engine (`docs/PAYROLL.md`)

## 1. Monthly Salary Calculation Flow

Monthly salary calculation runs automatically per employee and manager for each month from their joining date up to the current month:

- **Shift Attendance Payments**: Computed from verified shift attendance records (`attendance_events`) evaluated against `shift_payment_rules` (`PRESENT`, `LATE`, `HALF_DAY`, `ABSENT`).
- **Holiday & Off-Days Payments**: Computed from unworked paid public holidays and off-day counts evaluated against `unworked_payment_rules` (`HOLIDAY`, `WEEKLY_OFF`, `PAID_LEAVE`).
- **Completed Overtime**: Aggregated from completed & approved entries in the `overtime` table (`status` IN `APPROVED`, `COMPLETED`, `PAID`, `CLOSED`).
- **Gross & Net Totals**: Calculated as `Gross Salary = Shift Earnings + Holiday/Off-Day Earnings + Overtime Pay - Penalties/Deductions`.

---

## 2. Salary Payment Console & Month-by-Month Disbursal Flow
- **Direct Workforce Payment Roster**: Renders employee and manager rosters with live salary calculations from joining date up to current month.
- **Month Card Disbursements**: Each month card displays itemized Earned, Paid, and Due salary, shift attendance counts (Present, Half-Day, Leave, Absent, Holidays, Off-Days), and a dedicated **Pay Salary** / **Pay Again** button.
- **Payment Modal (`PayStaffModal`)**: Displays the selected month (e.g. `Jan 2025`), current due balance, and month payment history (listing previous disbursements, payment modes, reference numbers/UTRs, and dates). Supports partial or full payments via Bank Transfer, UPI, Cash, or Cheque with custom reference notes.
- **Ledger Transfer**: Payment confirmation moves wallet balances from `approved_unpaid_balance_cents` or `earned_balance_cents` to `paid_balance_cents` using double-entry idempotency-backed ledger transactions.

---

## 3. Itemized Payslips
Generated payslips include:
- Organization branding, name, tax/business ID.
- Staff/Manager details: Name, Staff Code, Department, Designation, Hire Date.
- Attendance summary: Total Days, Present Days, Late Days, Half Days, Leave Days, Unpaid Absences, Public Holiday Count, Weekly Off Count.
- Itemized Earnings: Base Shift Earnings (`shift_payment_rules`), Public Holiday & Weekly Off Pay (`unworked_payment_rules`), Completed Overtime Pay (`overtime`), Bonuses.
- Itemized Deductions: Late Penalties, Half-Day Deductions, Unpaid Leave Deductions, Advances.
- Net Payable Amount & Payment Reference History.
