# REST API Specifications (`docs/API.md`)

All API routes are versioned under `/api/v1`. Requests and responses follow standard JSON contracts validated by Zod schemas.

---

## 1. Authentication & Session Endpoints (`/api/v1/auth`)

### `POST /api/v1/auth/otp/request`
Requests a 6-digit email OTP. The response includes `devOtp` only when the server runs with `NODE_ENV=development` or `NODE_ENV=test`. OTPs are hashed in PostgreSQL and expire after 10 minutes.

### `POST /api/v1/auth/otp/verify`
Verifies email 6-digit OTP code and issues JWT credentials.
- For new Owner registration, include `role: "OWNER"` and the owner/organization/premises fields below. The server verifies the OTP and provisions the owner workspace as part of this request.
- **Request Body**:
  ```json
  {
    "email": "owner@company.com",
    "code": "123456",
    "role": "OWNER",
    "fullName": "Jane Doe",
    "organizationName": "Acme Corp",
    "timezone": "Asia/Kolkata",
    "currency": "INR",
    "premisesName": "Main Office",
    "radiusMeters": 50,
    "deviceName": "Google Pixel 8",
    "deviceType": "MOBILE",
    "osName": "Android 14"
  }
  ```

### `POST /api/v1/auth/google/exchange`
Exchanges Google server authorization code for application JWT credentials.

### `POST /api/v1/auth/google/owner-registration`
Verifies a Google ID token and creates a new Owner account, organization, membership, and initial premises atomically.

### `POST /api/v1/auth/refresh`
Rotates refresh token and returns a new 15-minute access token.

### `GET /api/v1/auth/me`
Retrieves currently authenticated user profile.

### `GET /api/v1/auth/devices`
Lists all logged devices and active session status for the authenticated user.

### `DELETE /api/v1/auth/devices/:deviceId`
Revokes a targeted logged device session by ID.

### `POST /api/v1/auth/logout`
Revokes the current device session `jti`.

### `POST /api/v1/auth/logout-all`
Revokes all active device sessions for the user across all devices.

---

## 2. Organization & Invitation Endpoints (`/api/v1/organizations`, `/api/v1/invitations`)

### `POST /api/v1/organizations`
Creates a new organization (Owner only).

### `POST /api/v1/invitations`
Sends an invitation to an Employee.

### `POST /api/v1/invitations/accept`
Accepts a single-use token and links the user to the organization.

---

## 3. Employee & Shift Management Endpoints (`/api/v1/employees`, `/api/v1/shifts`)

### `POST /api/v1/employees`
Creates an employee profile (`employees`) and sets salary structure (Owner only).

### `GET /api/v1/employees`
Lists organization employees with filtering and pagination.

### `POST /api/v1/shifts`
Creates a shift definition (`shifts`).

### `POST /api/v1/shifts/assignments`
Assigns a shift to employees for a specified date range (`employee_shift_assignments`).

---

## 4. Attendance & GPS Endpoints (`/api/v1/attendance`, `/api/v1/location`)

### `POST /api/v1/attendance/clock-in`
Submits a clock-in event with a GPS reading and the on-device face embedding. The backend performs the authoritative match against `multi_pose_face_templates`.

### `POST /api/v1/attendance/clock-out`
Submits clock-out event.

### `POST /api/v1/location/checks`
Submits periodic ~10-minute active-shift location reading (`attendance_location_pings`).

### `GET /api/v1/attendance`
Lists attendance records (`employee_attendance`).

---

## 5. Face Enrollment Endpoints (`/api/v1/face`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/face/status` | Current user's enrollment status |
| `POST` | `/api/v1/face/enroll/supervised` | Owner enrolls an employee in person (`multi_pose_face_templates`) |
| `POST` | `/api/v1/face/verify` | Verifies a live embedding against the enrolled template |

---

## 6. Leave Endpoints (`/api/v1/leave`)

### `GET /api/v1/leave`
Lists leave requests (`leave_requests`).

### `POST /api/v1/leave`
Creates a leave request.

### `POST /api/v1/leave/:id/review`
Approves or rejects a leave request.

---

## 7. Salary & Earning Ledger Endpoints (`/api/v1/salary`, `/api/v1/earnings`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/salary/unworked-rules` | Effective holiday / weekly-off / paid-leave rules (`unworked_payment_rules`) |
| `POST` | `/api/v1/salary/unworked-rules` | Creates an effective-dated unworked-day rule |
| `GET` | `/api/v1/earnings` | Retrieves append-only earning records (`staff_earning_ledger`) |

---

## 8. Reporting & Payroll Endpoints (`/api/v1/reports`, `/api/v1/payroll`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/reports/dashboard` | Role-aware dashboard summary |
| `GET` | `/api/v1/reports/attendance` | Attendance report (`format=json|csv|pdf`) |
| `GET` | `/api/v1/reports/payroll/:periodId` | Payroll report (`monthly_payrolls`) |

### `POST /api/v1/payroll`
Calculates and records monthly payroll and disbursements (`monthly_payrolls`, `payroll_transactions`).

---

## 9. Standard Error Response Contract

All errors return consistent JSON responses:
```json
{
  "statusCode": 400,
  "error": "Bad Request",
  "code": "INVALID_LOCATION_ACCURACY",
  "message": "GPS reading accuracy exceeds maximum allowed threshold of 30 meters",
  "timestamp": "2026-03-30T10:15:00Z"
}
```
