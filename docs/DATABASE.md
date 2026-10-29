# Database Architecture & Schema (`docs/DATABASE.md`)

## 1. Database Overview
The authoritative database is **PostgreSQL 16+**, accessed via **Drizzle ORM**. All business data enforces multi-tenant isolation through an `organization_id` foreign key referencing `organizations(id)`. The current schema uses the new unified model with `employees`, `employee_shift_assignments`, `employee_attendance`, `staff_earning_ledger`, and `monthly_payrolls`.

---

## 2. Core Tables

### Identity & Authentication
- **`users`** - User identities (email, fullName, phone, avatarUrl, tokenVersion)
- **`google_identities`** - Google OAuth linked accounts
- **`otp_requests`** - Email OTP verification codes
- **`logged_devices`** - Active sessions with refresh token hashes, per-device session tracking

### Organization & Structure
- **`organizations`** - Organizations with `owner_user_id`
- **`premises`** - Physical locations with geofence coordinates and radius
- **`employees`** - Staff and managers (linked to `users` optionally, have `employee_code`, `role` in `employee_role` enum)

### Shifts & Assignments
- **`shifts`** - Shift templates per premises with start/end times and half-day threshold
- **`employee_shift_assignments`** - Assignments linking employees to shifts with pay rates (`present_pay`, `absent_pay`, `leave_pay`, `half_day_pay`)

### Attendance & Monitoring
- **`employee_attendance`** - One row per shift occurrence or overtime occurrence. Tracks clock in/out, verification flags, GPS ping counters, worked minutes
- **`attendance_location_pings`** - Individual GPS location pings during active monitoring
- **`multi_pose_face_templates`** - Enrolled face embeddings (5 poses) for biometric verification

### Leave & Time Off
- **`leave_requests`** - Leave requests linked to shift assignments
- **`weekly_offs`** - Employee weekly off days
- **`holiday_calendars`** - Organization holidays

### Earnings & Payroll
- **`staff_earning_ledger`** - Append-only earning records (`SHIFT`, `HOLIDAY`, `OFF_DAY`, `OVERTIME`)
- **`overtime`** - Overtime requests with payment amounts
- **`monthly_payrolls`** - Monthly payroll summaries per employee
- **`payroll_deductions`** - Deductions on payrolls
- **`payroll_transactions`** - Payments made against payrolls
- **`unworked_payment_rules`** - Payment rules for holidays/off-days/paid leave
- **`monthly_attendance_stats`** - Aggregated monthly attendance statistics

### Operations
- **`clock_nonces`** - Nonces for secure clock operations
- **`audit_logs`** - Audit trail of system actions

---

## 3. Key Enums
- **`role`**: OWNER, MANAGER, STAFF
- **`employee_role`**: MANAGER, STAFF
- **`attendance_type`**: SHIFT, OVERTIME
- **`attendance_status`**: SCHEDULED, PRESENT, LATE, HALF_DAY, ABSENT, LEAVE, HOLIDAY, WEEKLY_OFF
- **`earning_type`**: SHIFT, HOLIDAY, OFF_DAY, OVERTIME
- **`location_check_result`**: IN_PREMISES, OUTSIDE, STALE, LOW_ACCURACY, MOCKED, UNKNOWN
- **`overtime_status`**: REQUESTED, APPROVED, REJECTED, IN_PROGRESS, COMPLETED, CANCELLED
- **`leave_status`**: PENDING, APPROVED, REJECTED, CANCELLED
- **`leave_type`**: PAID, UNPAID, SICK, LATE_CLOCK_IN, EARLY_CLOCK_OUT
- **`monthly_payroll_status`**: OPEN, FINALIZED, PAID
- **`unworked_day_type`**: HOLIDAY, WEEKLY_OFF, PAID_LEAVE
