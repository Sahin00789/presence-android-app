# Monthly Payroll & Salary Calculation Engine (`docs/PAYROLL.md`)

## 1. Monthly Salary Calculation Flow

Monthly salary calculation runs automatically for all employees (both Staff and Managers residing in `employees` table) for each month from their joining date up to the current month. **The Owner is NOT an employee, has NO attendance tracking, earns NO salary, and cannot receive payroll disbursements; the Owner holds exclusive rights to review, disburse, and lock monthly payroll (`monthly_payrolls`).**

- **Shift Attendance Payments**: Computed from verified shift attendance records (`employee_attendance`) evaluated against shift assignment pay rates (`present_pay`, `half_day_pay`, `leave_pay`, `absent_pay`).
- **Holiday & Off-Days Payments**: Computed from unworked paid public holidays and weekly off counts evaluated against `unworked_payment_rules` (`HOLIDAY`, `WEEKLY_OFF`, `PAID_LEAVE`).
- **Completed Overtime**: Aggregated from completed & approved entries in the `overtime` table.
- **Gross, Deductions & Net Totals**: Stored in `monthly_payrolls` with itemized breakdown in `staff_earning_ledger`, `payroll_deductions`, and settled via `payroll_transactions`.

---

## 2. Salary Payment Console & Month-by-Month Disbursal Flow
- **Direct Workforce Payment Roster**: Renders employee and manager rosters with live salary calculations from joining date up to current month.
- **Month Card Disbursements**: Each month card displays itemized Earned, Deducted, Paid, and Due salary, shift attendance counts, and a dedicated **Pay Salary** / **Pay Again** button.
- **Payment Modal (`PayStaffModal`)**: Displays the selected month (e.g. `Jan 2025`), current due balance, and month payment history (listing previous disbursements, payment modes, reference numbers, and dates via `payroll_transactions`).
- **Ledger Settlement**: Payment confirmation inserts a record into `payroll_transactions` and updates `monthly_payrolls` statuses (`OPEN`, `FINALIZED`, `PAID`).

---

## 3. Itemized Payslips
Generated payslips include:
- Organization branding, name, timezone, currency.
- Employee details: Name, Staff Code, Department, Designation, Hire Date.
- Attendance summary: Present Days, Absent Days, Leave Days, Half Days, Public Holiday Count, Weekly Off Count.
- Itemized Earnings: Shift Earnings (`staff_earning_ledger`), Public Holiday & Weekly Off Pay, Completed Overtime Pay (`overtime`).
- Itemized Deductions: Penalties, Advances, Deductions (`payroll_deductions`).
- Net Payable Amount & Payment Reference History (`payroll_transactions`).
