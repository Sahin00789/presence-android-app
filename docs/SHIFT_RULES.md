# Shift Management & Overtime Policies (`docs/SHIFT_RULES.md`)

## 1. Shift Template & Premises Scoping
Organizations define reusable shift templates in `shift_templates`:
- `name`: e.g. "Morning Shift", "Night Shift".
- `premises_id`: Optional scoping to a specific business premises.
- `start_time`: e.g. `09:00:00`.
- `end_time`: e.g. `17:30:00`.

---

## 2. Shift Assignments & Weekday Filters
- **Premises Selection at Top**: Shifts management UI provides a top premises filter bar to view shift roster assignments and templates by premises.
- **Independent Weekday Assignments (`days_of_week`)**: Day-of-week assignments are fully independent. Each day of the week (Sunday 0 through Saturday 6) can have its own distinct shift type, timing range, and attendance/payment rates (e.g. Morning Shift Mon–Thu, Evening Shift Fri, Half-Day Shift Sat, Weekly Off Sun).
- **Non-Overlapping Weekday Conflict Check**: Multiple shift assignments for the same staff member do not conflict if their `days_of_week` sets are disjoint. Overlap validation evaluates both calendar date ranges and weekday intersections.
- **Direct Assignment from Staff/Manager Cards**: Managers and Owners can assign shifts directly from any Staff or Manager card in the dashboard using the 7-day card modal.

---

## 3. Overnight Shifts & Midnight Crossing

For shifts crossing midnight (e.g. 10:00 PM to 6:00 AM):
1. **Attendance Date Assignment**: The attendance record is bound to the calendar date on which the shift **began**.
2. **Payroll Month Boundary**: If an overnight shift begins on the last day of the month (e.g. March 31, 10 PM) and ends on April 1 (6 AM), all hours worked belong to the March payroll period.
3. **Overlapping Shift Prevention**: Drizzle SQL constraints prevent assigning overlapping shift templates to the same Staff member for any given time window.

---

## 4. Off-Day Schedules & Holidays
- **Weekly Offs**: Configured per staff member (e.g. Sunday or Saturday-Sunday).
- **Public Holidays**: Configured in `holiday_calendars`.
- **Off-Day / Holiday Work**: If Staff clocks in on a scheduled weekly off or public holiday, the attendance engine records it as a normal present day. There are no separate worked-holiday or worked-off-day rule types or premium multipliers.

---

## 5. Shift Attendance Payment Status Rules
- **Shift-Level Payment Scoping**: Attendance payment rules (Present Pay, Half-Day Pay, Leave Pay, Absent Penalty) are assigned per **Shift / Shift Template**, NOT bound to calendar days.
- **Attendance Status Rates**: Each shift template carries its own attendance status payment structure:
  - **Present Pay** (e.g., ₹600.00 / shift)
  - **Half-Day Pay** (e.g., ₹300.00 / shift)
  - **Leave Pay** (e.g., ₹600.00 / shift)
  - **Absent Penalty** (e.g., ₹0.00)
- **Automatic Effective Dates**: API endpoints for shift payment rules default `effectiveFrom` to the current date (`YYYY-MM-DD`) when omitted, eliminating manual client-side date selection.
