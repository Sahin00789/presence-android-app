# Database Schema & Overview

- **`organizations`** - Organization details, `owner_user_id` referencing `users.id`.
- **`premises`** - Premises locations, geofences (latitude, longitude, radius_meters).
- **`users`** - User identity accounts (`email`, `google_id`, `role: OWNER | EMPLOYEE`).
- **`employees`** - Workforce employees (linked to `users`, `employee_code`, `role: EMPLOYEE`, `premises_id`).
- **`shifts`** & **`employee_shift_assignments`** - Shift definitions and employee assignments.
- **`employee_attendance`** & **`attendance_location_pings`** - Clock-in/out records and GPS pings.
- **`staff_earning_ledger`** - Append-only ledger of earned salary.
- **`monthly_payrolls`** - Payroll lock records and disbursements.
