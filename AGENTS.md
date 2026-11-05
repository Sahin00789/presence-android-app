# AGENTS.md - Employee Attendance, Shift Management & Salary Ledger System (`presence`)

## 1. Project Purpose & Scope
The **Employee Attendance, Shift Management & Salary Ledger System** (`presence`) is a production-ready, multi-tenant mobile and backend platform designed for business workforce management, automated GPS and facial verification attendance tracking, flexible shift scheduling, append-only employee earning ledger accounting, and monthly payroll processing.

### Key Roles & Data Representation
1. **Owner**: Independent self-registration, organization creation, premises configuration, direct addition of Employees across all premises, high-level organization oversight, payroll locking (`monthly_payrolls`), and approval of sensitive operational/financial adjustments.
   - *Data Storage*: Represented by `organizations.owner_user_id` referencing `users.id`. **Owners are NOT employees** and do not reside in the `employees` table.
2. **Employee**: Directly added by Owner to an assigned premises. Stored in the `employees` table with `role = 'EMPLOYEE'` and `premises_id`. Signs in via Email OTP or Google OAuth, performs face + GPS clock-in/clock-out, participates in active-shift GPS checks (`attendance_location_pings`), views shift schedules, tracks earned salary (`staff_earning_ledger`), and accesses monthly payslips.

---

## 2. Technology Stack & Key Libraries

### Android Client (`/app` or `/android`)
- **Language**: Kotlin (Strict, idiomatic, coroutines + flow).
- **UI Framework**: Jetpack Compose with Material 3 design components.
- **Architecture**: MVVM with Clean Architecture (UI -> ViewModel -> UseCase/Repository -> Remote/Local DataSource).
- **Dependency Injection**: Hilt (`com.google.dagger:hilt-android`).
- **Networking**: Retrofit 2 + OkHttp 4 + KotlinX Serialization.
- **Local Database**: Room DB for local caching & offline attendance queueing.
- **Local Storage**: DataStore (Preferences & Proto) for secure settings/session storage.
- **Biometrics & Camera**: CameraX (`androidx.camera:*`) + ONNX Runtime Mobile (`com.microsoft.onnxruntime:onnxruntime-android`) for local face verification.
- **Location**: Google Fused Location Provider API (`com.google.android.gms:play-services-location`).
- **Background Tasks**: WorkManager + Foreground Services for periodic ~10-minute GPS checks.
- **Security**: Android Keystore API for cryptographic token and biometric key protection.
- **Auth**: Email 6-Digit OTP & Google OAuth 2.0 Single Sign-On.

### Backend Server (`/server`)
- **Runtime & Language**: Node.js LTS with TypeScript (Strict mode).
- **Web Framework**: Fastify 5 + `fastify-type-provider-zod`.
- **Database & ORM**: PostgreSQL 16+ (or NeonDB Serverless) with Drizzle ORM (`drizzle-orm` + `drizzle-kit`).
- **PostgreSQL Client**: `postgres` (pnpm package `postgres`), NOT `pg`. Includes 4-minute keep-alive pings to prevent serverless cold starts.
- **Cache & Rate Limiting**: In-process sliding-window rate limiter & memory deduplication (PostgreSQL-only, zero Redis requirement).
- **Validation**: Zod for request/response schemas and environment variables.
- **Real-Time Delivery**: `@fastify/websocket` (`/api/v1/events/ws`) for live ledger, attendance & notification updates.
- **Logging**: Fastify default Pino structured JSON logging.

---

## 3. Architecture & Coding Conventions

### Android Conventions
1. **Package Name**: `com.als/presence`
2. **Directory Layout**:
   - `core/`: Shared UI components, design tokens (`designsystem/`), network, database, security, location, face verification engine.
   - `feature/`: Feature modules (`auth`, `dashboard`, `owner`, `staff`, `shifts`, `attendance`, `payroll`, `reports`).
3. **Data Flow**: `Compose UI` -> `ViewModel` -> `UseCase` -> `Repository` -> `DataSource`.
4. **State Management**: Use `StateFlow` and immutable data models (`data class`, `UiState`). UI components observe state via `collectAsStateWithLifecycle()`.
5. **No Direct Retrofit in UI**: Compose screens and ViewModels must never invoke Retrofit or Room directly; always go through a clean Repository interface.

### Shifts UI & Staff Card Conventions
1. **Top Premises Selection**: Shifts UI features a Premises Selector at the top ("All Premises" + list of premises chips) to filter roster assignments and templates by premises.
2. **Weekday Assignments & Separation**: Shift definitions (`shifts`) contain `dayName` (`day_name`) specifying the weekday name, alongside start time, end time, and half-day thresholds.
3. **Employee Card Actions**: Each Employee card features an **"Assign shift"** action chip that launches `AssignShiftModal` pre-filled for that employee.

---

## 4. Authentication & Single-Device Concurrency
1. **Supported Providers**: Email 6-Digit OTP (`/api/v1/auth/otp/request` & `/api/v1/auth/otp/verify`) AND Google OAuth 2.0 (`/api/v1/auth/google/exchange`) are supported authentication providers for all roles (`OWNER`, `EMPLOYEE`).
2. **Development Testing Mode**: OTP requests in development/testing mode return `devOtp` (e.g. `123456`) in the response payload for instant local testing.
3. **Role Account Verification & Access Rules**:
   - **Registered Accounts / Direct Additions**: Authenticates session and logs directly into their assigned workspace (`OWNER` or `EMPLOYEE`).
   - **Unregistered Owner**: Prompts the user to complete Owner & Organization Registration.
   - **Unregistered Employee**: Refuses sign-in with an explicit message: `"You are not registered. Please contact your Administrator to receive an invitation."`
4. **Session Credentials, Logged Devices & Single-Device Rule**:
   - Short-lived Access Token (JWT, 15-min expiry).
   - Rotating Refresh Token (3-month / 90-day sliding expiry, single device per user).
   - Active sessions are recorded in `logged_devices` table in PostgreSQL with SHA-256 hashed refresh tokens (`refresh_token_hash`).
   - **Single-Device Concurrency Enforcement**: For ALL roles (`OWNER`, `EMPLOYEE`), only **1 active device session** is permitted at a time. Authenticating on a new/latest device automatically logs out and revokes all prior active device sessions in PostgreSQL `logged_devices`.

---

## 5. Role & Multi-Tenant Authorization Rules
1. **Multi-Tenancy**: Every business domain model must belong to an `organization_id`.
2. **Organization-Level vs Premises-Level Scope**:
   - **Owner**: Holds organization-level (`organization_id`) administration rights across all premises (organization creation, direct addition of Employees with mandatory premises assignment, global premises configuration, payroll locking, and financial approvals). Owners are NOT employees.
   - **Employee (`employees`)**: Premises-level workforce scoped by `premises_id` (`role = 'EMPLOYEE'`). Employees have their shifts, attendance, and salary tracked.
   - **Shifts (`shifts`) & Assignments (`employee_shift_assignments`)**: Premises-level entities tied to specific premises (`premises_id`).
3. **Strict Verification**: Every API request must validate the caller's JWT, confirm active organization membership, and check required role permissions.
4. **Never Trust Client Inputs**: Organization IDs and user roles passed from the frontend must be ignored in favor of authenticated session claims.

---

## 6. Database, Ledger & Payment Rules
1. **PostgreSQL Authoritative**: PostgreSQL is the single source of truth for all business data. Redis failure or restart must never cause data loss.
2. **Financial Precision**: All monetary values must be stored in exact whole integer units in Indian Rupees (INR / ₹). Floating-point types, cents, paise, and decimals are strictly forbidden.
3. **Employees Table (`employees`)**: Represents workforce employees (`role: EMPLOYEE`) linked to `organizations` and scoped to `premises_id`. Owners are represented by `organizations.owner_user_id` referencing `users.id` and are NOT employees.
4. **Shifts & Assignments (`shifts`, `employee_shift_assignments`)**: Premises-level shift definitions and assignment records scoped by `premises_id`, containing pay rates (`present_pay`, `absent_pay`, `leave_pay`, `half_day_pay`).
5. **Unworked Payment Rules (`unworked_payment_rules`)**: Configures holiday, weekly off-day, and paid-leave payment amounts/multipliers (`HOLIDAY`, `WEEKLY_OFF`, `PAID_LEAVE`). Worked holidays and worked off-days are paid as normal present days; no separate worked-day rule types exist.
6. **Overtime Table (`overtime`)**: Overtime entries are created with individual payment amounts (`payment_inr`), status, dates, and planned/worked minutes.
7. **Monthly Salary Calculation & Payroll Engine (`monthly_payrolls`)**:
   - Calculates shift earnings, holiday/off-day pay, completed overtime, and payroll disbursements (`payroll_transactions`) for all employees. Owner possesses exclusive rights for payroll locking (`monthly_payrolls`).
8. **Staff Earning Ledger (`staff_earning_ledger`)**:
   - Append-only earning records (`SHIFT`, `HOLIDAY`, `OFF_DAY`, `OVERTIME`) linking earnings directly to source attendance/shift records for all employees.

---

## 7. Attendance, GPS & Local Face Verification
1. **Premises Geofence**: Default radius of **50 metres**. Distance evaluated via geodesic FusedLocation readings.
2. **Clock-In Requirements**: Valid geofence check + local ONNX face verification match.
3. **Periodic GPS Monitoring & Manual Attendance Rules**:
   - Active shift schedules trigger location checks approximately every 10 minutes (`attendance_location_pings`).
   - **Three Consecutive Outside Pings (`outside count > 3`)**: Automatically raises a monitoring exception alert and notifies Owner.
   - **Manual Attendance Marking & Action Buttons**: Resolution action buttons appear on the Owner dashboard and remain visible and active **until the shift or overtime is completely finished/completed**.
   - **Manual Options**: Owner can manually mark attendance as `ABSENT`, `HALF_DAY`, `LEAVE`, or resume tracking (`RESUME`).
   - **Default Present Policy (No Action)**: If the owner **does not press any button** before the shift or overtime session completes, the attendance status automatically **defaults to Present** (`PRESENT`).
4. **Local Biometrics**:
   - ONNX Runtime Mobile infers face embeddings on-device using CameraX input against `multi_pose_face_templates`.
   - Only 128-dimensional numerical embeddings are transmitted/stored securely. Raw facial images are discarded after feature extraction.

---

## 8. Build, Lint & Verification Commands
- **Android Build**: `./gradlew assembleDebug`
- **Android Unit Tests**: `./gradlew test`
- **Android Lint**: `./gradlew lint`
- **Backend Install**: `cd server && pnpm install`
- **Backend Typecheck**: `cd server && pnpm run typecheck`
- **Backend Build**: `cd server && pnpm run build`
- **Backend Tests**: `cd server && pnpm run test`
- **Drizzle DB Migration**: `cd server && pnpm run db:migrate`

---

## 9. Prohibited Shortcuts & Critical Constraints
- **NO** floating-point numbers for money calculations.
- **NO** raw password authentication or bypass endpoints in production.
- **NO** hardcoded secrets or backend OAuth client secrets in Android code.
- **NO** direct DB balance updates without ledger/earning entries.
- **NO** client-side salary or payroll calculation engine; backend is authoritative.
- **NO** shell-based file editing tools that bypass IDE file buffers.
