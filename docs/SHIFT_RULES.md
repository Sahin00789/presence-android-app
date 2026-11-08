# Shift Management & Overtime Policies (`docs/SHIFT_RULES.md`)

## 1. Shift Definition & Premises Scoping
Organizations define reusable shifts in `shifts`:
- `shiftName`: e.g. "Morning Shift", "Night Shift".
- `premisesId`: Scoping to a specific business premises.
- `startTime`: e.g. `09:00:00`.
- `endTime`: e.g. `17:30:00`.
- `halfDayThreshold`: e.g. `11:00:00`.

---

## 2. Shift Assignments & Weekday Filters (`employee_shift_assignments`)
- **Single Premises Roster**: Shifts management UI manages shift definitions and roster assignments for the single premises workspace without requiring premises filtering.
- **Independent Weekday Assignments**: Day-of-week assignments support specific days and rates (`present_pay`, `absent_pay`, `leave_pay`, `half_day_pay`).
- **Direct Assignment from Employee Cards**: Owners can assign shifts directly from any Employee card in the dashboard.

---

## 3. Overnight Shifts & Midnight Crossing

For shifts crossing midnight:
1. **Attendance Date Assignment**: The attendance record is bound to the calendar date on which the shift **began** (`employee_attendance`).
2. **Payroll Month Boundary**: All hours worked belong to the calendar month period of the work date.

---

## 4. Off-Day Schedules & Holidays
- **Weekly Offs**: Automatically derived per employee in `weekly_offs` based on active shift assignments. Any weekday with **no active shift assigned** for that employee is treated as a weekly off. Weekly off configuration is read-only via API (GET only); assignment changes sync weekly offs automatically.
- **Public Holidays**: Configured in `holiday_calendars` via the separate Holidays module (`/api/v1/holidays`).
- **Off-Day / Holiday Work**: If Employee clocks in on a scheduled weekly off or public holiday, the attendance engine records it as a normal present day.
