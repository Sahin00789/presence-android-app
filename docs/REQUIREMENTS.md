# System Requirements & SLA (`docs/REQUIREMENTS.md`)

## 1. Functional Requirements

### 1.1 Authentication & Role Management
- **FR-AUTH-1**: The system must support Google Sign-In as the primary authentication method and email 6-digit OTP as an alternative for `OWNER`, `MANAGER`, and `STAFF`.
- **FR-AUTH-1a**: Google authentication must use Google only for identity verification and basic profile data; the app must not request Gmail, Drive, contacts, or unrelated Google data.
- **FR-AUTH-1b**: Email OTP authentication must clearly explain that a one-time code is sent to the provided email and does not use Google account data.
- **FR-AUTH-2**: Self-registration is allowed **only** for the Owner role.
- **FR-AUTH-3**: Managers must be added by an Owner. An account and staff profile are immediately created on the backend.
- **FR-AUTH-4**: Staff must be added by an Owner or Manager. An account and staff profile are immediately created on the backend with employment details attached.
- **FR-AUTH-5**: Single-use invitation tokens must expire after 48 hours and enforce organization isolation.

### 1.2 Biometric & Location Attendance
- **FR-ATT-1**: Clock-in requires simultaneous 50-metre premises geofence validation and local ONNX face recognition.
- **FR-ATT-2**: Face recognition must run on-device via CameraX and ONNX Runtime Mobile, extracting a 128/512-dim embedding.
- **FR-ATT-3**: The client must perform liveness checks (quality, head pose, lighting) before face verification.
- **FR-ATT-4**: The app must perform periodic background GPS checks approximately every 10 minutes during active shifts.
- **FR-ATT-5**: Three consecutive failed GPS checks must generate a location exception and alert both Manager and Staff.
- **FR-ATT-6**: GPS failure must **not** automatically mark staff as absent; manual Manager review is mandatory.

### 1.3 Shifts, Leave & Holidays
- **FR-SFT-1**: Support fixed, flexible, and overnight shift templates crossing midnight boundaries.
- **FR-SFT-2**: Support grace periods for late arrivals and half-day working duration thresholds.
- **FR-SFT-3**: Manage weekly-off schedules, organization public holidays, and paid/unpaid leave requests.

### 1.4 Salary Wallet & Monthly Payroll
- **FR-WAL-1**: Maintain a personal real-time salary wallet for every employed Manager and Staff member.
- **FR-WAL-2**: Wallet balances must be calculated dynamically from an append-only double-entry transaction ledger.
- **FR-WAL-3**: Support 5 compensation structures: Fixed Monthly, Daily, Hourly, Per-Shift, and Mixed.
- **FR-PAY-1**: Support monthly payroll lifecycle: Draft -> Calculating -> Review -> Approval -> Locked -> Paid -> Closed.
- **FR-PAY-2**: Generate itemized payslips detailing base earnings, overtime, holiday pay, off-day work, deductions, and net salary.

---

## 2. Non-Functional Requirements & SLA

### 2.1 Security & Compliance
- **NFR-SEC-1**: All API communication must enforce TLS 1.3/HTTPS.
- **NFR-SEC-2**: Raw biometric images must be discarded immediately after feature extraction. Embeddings must be encrypted in PostgreSQL.
- **NFR-SEC-3**: All monetary amounts must use integer minor units (paise/cents) or exact SQL `NUMERIC`. Zero floating-point math.

### 2.2 Performance & Reliability
- **NFR-PERF-1**: Face verification inference on Android device must complete in under 500ms.
- **NFR-PERF-2**: API clock-in endpoint response time must be under 1.5 seconds.
- **NFR-PERF-3**: Database transactions for wallet updates must enforce strict serializable/read-committed isolation.

### 2.3 Battery & System Impact
- **NFR-BAT-1**: Active-shift background GPS monitoring must consume less than 3% battery per 8-hour shift.
- **NFR-BAT-2**: WorkManager and Fused Location API must utilize adaptive power-friendly location requests.
