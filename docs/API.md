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
  For existing-account sign-in, `role` identifies the requested workspace role and the registration fields may be omitted.

### `POST /api/v1/auth/google/exchange`
Exchanges Google server authorization code for application JWT credentials.
- **Request Body**:
  ```json
  {
    "code": "4/0AeaYSH...",
    "redirectUri": null,
    "deviceName": "Samsung Galaxy S23",
    "deviceType": "MOBILE",
    "osName": "Android 14"
  }
  ```
  `redirectUri` is optional and accepts `null`; when omitted or null, the server uses its configured Google redirect URI.
- **Response (200 OK)**:
  ```json
  {
    "accessToken": "eyJhbGciOi...",
    "expiresIn": 900,
    "refreshToken": "refresh:u-123:jti-456",
    "user": {
      "id": "u-123",
      "email": "owner@company.com",
      "fullName": "Jane Doe",
      "role": "OWNER",
      "organizationId": "org-456",
      "organizationName": "Acme Corp"
    }
  }
  ```

### `POST /api/v1/auth/google/owner-registration`
Verifies a Google ID token and creates a new Owner account, organization, membership, and initial premises atomically. Use this only for a new owner; existing accounts should sign in with `/api/v1/auth/google/exchange`.
- **Request Body**:
  ```json
  {
    "idToken": "google-id-token",
    "organizationName": "Acme Corp",
    "timezone": "Asia/Kolkata",
    "currency": "INR",
    "premisesName": "Main Office",
    "radiusMeters": 50,
    "deviceName": "Samsung Galaxy S23",
    "deviceType": "MOBILE",
    "osName": "Android 14"
  }
  ```
- **Response**: Same JWT and user shape as `/api/v1/auth/google/exchange`.

### `POST /api/v1/auth/refresh`
Rotates refresh token and returns a new 15-minute access token.
- **Request Body**:
  ```json
  {
    "refreshToken": "refresh:u-123:jti-456",
    "deviceName": "Google Pixel 8"
  }
  ```

### `GET /api/v1/auth/me`
Retrieves currently authenticated user profile.

### `GET /api/v1/auth/devices`
Lists all logged devices and active session status for the authenticated user.
- **Response (200 OK)**:
  ```json
  {
    "devices": [
      {
        "id": "dev-001",
        "userId": "u-123",
        "sessionJti": "jti-456",
        "deviceName": "Google Pixel 8",
        "deviceType": "MOBILE",
        "osName": "Android 14",
        "ipAddress": "192.168.1.10",
        "userAgent": "PresenceAndroid/1.0",
        "isActive": true,
        "isCurrentDevice": true,
        "lastActiveAt": "2026-03-30T10:00:00Z",
        "createdAt": "2026-03-30T10:00:00Z",
        "revokedAt": null
      }
    ]
  }
  ```

### `DELETE /api/v1/auth/devices/:deviceId`
Revokes a targeted logged device session by ID.

### `POST /api/v1/auth/logout`
Revokes the current device session `jti` in Redis and marks device session inactive in DB.

### `POST /api/v1/auth/logout-all`
Revokes all active device sessions for the user across all devices.

---

## 2. Organization & Invitation Endpoints (`/api/v1/organizations`, `/api/v1/invitations`)

### `POST /api/v1/organizations`
Creates a new organization (Owner only).

### `POST /api/v1/invitations`
Sends an invitation to a Manager or Staff member.
- **Request Body**:
  ```json
  {
    "email": "manager@company.com",
    "role": "MANAGER"
  }
  ```

### `POST /api/v1/invitations/accept`
Accepts a single-use token and links the user's Google sub to the organization.

---

## 3. Staff & Shift Management Endpoints (`/api/v1/staff`, `/api/v1/shifts`)

### `POST /api/v1/staff`
Creates a staff employment profile and sets salary structure (Manager only).

### `GET /api/v1/staff`
Lists organization staff with filtering and pagination.

### `POST /api/v1/shifts/templates`
Creates a shift template (start_time, end_time, break_minutes, grace_minutes).

### `POST /api/v1/shifts/assignments`
Assigns a shift template to staff members for a specified date range.

---

## 4. Attendance & GPS Endpoints (`/api/v1/attendance`, `/api/v1/location`)

### `POST /api/v1/attendance/clock-in`
Submits a clock-in event with a GPS reading and the on-device face embedding. The backend performs the authoritative match against the encrypted enrolled template.
- **Request Body**:
  ```json
  {
    "latitude": 37.7749,
    "longitude": -122.4194,
    "accuracyMeters": 8.5,
    "embeddingVector": [0.012, -0.045, ...],
    "modelVersion": "onnx-mobile-v1",
    "isMocked": false,
    "timestamp": "2026-03-30T09:00:00Z"
  }
  ```

### `POST /api/v1/attendance/clock-out`
Submits clock-out event.

### `POST /api/v1/attendance/clock-in/manual`
Raises a `FACE_MISMATCH` (or location) exception for Manager/Owner review instead of silently marking attendance.

### `POST /api/v1/location/checks`
Submits periodic ~10-minute active-shift location reading.

### `GET /api/v1/attendance/exceptions`
Lists unresolved attendance and GPS exceptions.

### `POST /api/v1/attendance/corrections`
Submits or resolves a manual attendance correction request.

---

## 5. Face Enrollment Endpoints (`/api/v1/face`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/face/status` | Current user's enrollment status |
| `GET` | `/api/v1/face/staff/:staffProfileId/status` | Owner/Manager view of an employee's status |
| `POST` | `/api/v1/face/enroll/supervised` | Owner/Manager enrolls an employee in person |
| `POST` | `/api/v1/face/verify` | Verifies a live embedding against the enrolled template |
| `DELETE` | `/api/v1/face/enrollment/:staffProfileId` | Owner/Manager resets an enrollment |

Enrollment accepts only 128/512-dimensional finite embeddings. Staff cannot self-enroll.

---

## 6. Leave Endpoints (`/api/v1/leave`)

### `GET /api/v1/leave`
Staff see their own requests; Owners and Managers see the organization queue.

### `POST /api/v1/leave`
Creates a leave request (`type` = `PAID` | `UNPAID` | `SICK`).

### `POST /api/v1/leave/:leaveRequestId/review`
Approves or rejects a request (`decision` = `APPROVED` | `REJECTED`, optional `note`).

### `DELETE /api/v1/leave/:leaveRequestId`
Cancels a pending request owned by the caller.

---

## 7. Salary Rule Configuration (`/api/v1/salary`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/salary/shift-rules` | Effective per-shift attendance payment rules |
| `POST` | `/api/v1/salary/shift-rules` | Creates an effective-dated shift payment rule |
| `GET` | `/api/v1/salary/unworked-rules` | Effective holiday / weekly-off / paid-leave rules |
| `POST` | `/api/v1/salary/unworked-rules` | Creates an effective-dated unworked-day rule |

Rules are scoped by `organizationId`, optionally by `staffProfileId`/`shiftTemplateId`, and are resolved by the payroll engine on the period start date.

---

## 8. Reporting Endpoints (`/api/v1/reports`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/reports/dashboard` | Role-aware dashboard summary |
| `GET` | `/api/v1/reports/attendance` | Attendance report (`format=json|csv|pdf`) |
| `GET` | `/api/v1/reports/payroll/:periodId` | Payroll report (Owner/Manager) |
| `GET` | `/api/v1/reports/wallet` | Wallet liability report |
| `GET` | `/api/v1/reports/audit` | Audit trail report (Owner/Manager) |

Reports return `Content-Disposition: attachment` for CSV/PDF and use integer paise internally.

---

## 9. Wallet & Payroll Endpoints (`/api/v1/wallet`, `/api/v1/payroll`)

### `GET /api/v1/wallet/summary`
Retrieves current wallet breakdown (Earned, Approved Unpaid, Paid, Advances).

### `GET /api/v1/wallet/transactions`
Retrieves paginated double-entry wallet transaction history.

### `POST /api/v1/payroll/periods`
Opens a new monthly payroll period.

### `POST /api/v1/payroll/periods/:id/calculate`
Executes background salary calculation engine for the payroll period.

### `POST /api/v1/payroll/periods/:id/approve`
Owner approves and locks the calculated payroll period.

### `POST /api/v1/payroll/items/:id/payments`
Records a salary payment disbursement against a payroll item. Supports partial and multiple disbursements per month up to the total net payable amount.
- **Request Body**:
  ```json
  {
    "amountCents": 1500000,
    "paymentMethod": "BANK_TRANSFER",
    "referenceNumber": "UTR-9876543210"
  }
  ```
- **Supported Payment Methods**: `BANK_TRANSFER`, `UPI`, `CASH`, `CHEQUE`.
- **Ledger Transfer**: Debits `APPROVED_UNPAID` (or `EARNED` balance) and credits `PAID` bucket via append-only idempotency-backed transactions.

---

## 10. Standard Error Response Contract

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
