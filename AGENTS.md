# AGENTS.md - Staff Attendance, Shift Management & Salary Wallet System (`presence`)

## 1. Project Purpose & Scope
The **Staff Attendance, Shift Management & Salary Wallet System** (`presence`) is a production-ready, multi-tenant mobile and backend platform designed for business staff management, automated GPS and facial verification attendance tracking, flexible shift scheduling, real-time salary wallet ledger accounting, and monthly payroll processing.

### Key Roles
1. **Owner**: Independent self-registration, organization creation, premises configuration, Manager creation/deactivation, high-level business oversight, and approval of sensitive operational/financial adjustments.
2. **Manager**: Created/invited by Owner. Handles daily operations, creates Staff, assigns shifts, reviews attendance exceptions, resolves manual attendance corrections, manages leave, prepares monthly payroll, and records approved payments.
3. **Staff**: Invited by Manager. Signs in via Email OTP or Google OAuth, completes local face enrollment, performs face + GPS clock-in/clock-out, participates in active-shift GPS checks, views shift schedules, tracks earned salary and wallet balances, and accesses monthly payslips.

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
- **Real-Time Delivery**: `@fastify/websocket` (`/api/v1/events/ws`) for live wallet, attendance & notification updates.
- **Logging**: Fastify default Pino structured JSON logging.

---

## 3. Architecture & Coding Conventions

### Android Conventions
1. **Package Name**: `com.als/presence`
2. **Directory Layout**:
   - `core/`: Shared UI components, design tokens (`designsystem/`), network, database, security, location, face verification engine.
   - `feature/`: Feature modules (`auth`, `dashboard`, `owner`, `manager`, `staff`, `shifts`, `attendance`, `wallet`, `payroll`, `reports`).
3. **Data Flow**: `Compose UI` -> `ViewModel` -> `UseCase` -> `Repository` -> `DataSource`.
4. **State Management**: Use `StateFlow` and immutable data models (`data class`, `UiState`). UI components observe state via `collectAsStateWithLifecycle()`.
5. **No Direct Retrofit in UI**: Compose screens and ViewModels must never invoke Retrofit or Room directly; always go through a clean Repository interface.

### Shifts UI & Staff Card Conventions
1. **Top Premises Selection**: Shifts UI features a Premises Selector at the top ("All Premises" + list of premises chips) to filter roster assignments and templates by premises.
2. **Weekday Assignments & Separation**: Shift assignments support specific days of week (`daysOfWeek`: Sun [0] to Sat [6]). Assignments are filtered/separated by weekday on screen with helper presets (`Mon–Fri`, `Sat & Sun`, `Everyday`).
3. **Staff & Manager Card Actions**: Each Staff and Manager card features an **"Assign shift"** action chip that launches `AssignShiftModal` pre-filled for that staff member or manager.

---

## 4. Authentication, Google OAuth & Email OTP Verification
1. **Supported Providers**: Email 6-Digit OTP (`/api/v1/auth/otp/request` & `/api/v1/auth/otp/verify`) AND Google OAuth 2.0 (`/api/v1/auth/google/exchange`) are supported authentication providers for all roles (`OWNER`, `MANAGER`, `STAFF`).
2. **Development Testing Mode**: OTP requests in development/testing mode return `devOtp` (e.g. `123456`) in the response payload for instant local testing.
3. **Mandatory Identity Verification for Registration**:
   - Organization & Owner Registration requires email identity verification via either Email OTP or Google OAuth BEFORE the registration wizard can be submitted and provisioned.
4. **Role Account Verification & Access Rules**:
   - **Registered Accounts / Invited Members**: Authenticates session and logs directly into their assigned workspace (`OWNER`, `MANAGER`, or `STAFF`).
   - **Unregistered Owner**: Prompts the user to complete Owner & Organization Registration.
   - **Unregistered Manager / Staff**: Refuses sign-in with an explicit message: `"You are not registered. Please contact your Administrator to receive an invitation."`
5. **Session Credentials, Logged Devices & Single-Device Rule**:
   - Short-lived Access Token (JWT, 15-min expiry).
   - Rotating Refresh Token (7-day expiry).
   - Active sessions are recorded in `logged_devices` table in PostgreSQL with SHA-256 hashed refresh tokens (`refresh_token_hash`).
   - **Single-Device Concurrency Enforcement**: For ALL roles (`OWNER`, `MANAGER`, `STAFF`), only **1 active device session** is permitted at a time. Authenticating on a new/latest device automatically logs out and revokes all prior active device sessions in PostgreSQL `logged_devices`.
   - Session Management APIs: Device list (`GET /api/v1/auth/devices`) and targeted revocation (`DELETE /api/v1/auth/devices/:deviceId`).

---

## 5. Role & Multi-Tenant Authorization Rules
1. **Multi-Tenancy**: Every business domain model must belong to an `organization_id`.
2. **Strict Verification**: Every API request must validate the caller's JWT, confirm active organization membership, and check required role permissions.
3. **Never Trust Client Inputs**: Organization IDs and user roles passed from the frontend must be ignored in favor of authenticated session claims.
4. **Owner Safeguards**: Owner dashboards possess exclusive write rights for Organization creation, Manager invitations, Premises configuration, and sensitive financial/attendance approval escalations.

---

## 6. Database, Ledger & Payment Rules
1. **PostgreSQL Authoritative**: PostgreSQL is the single source of truth for all business data. Redis failure or restart must never cause data loss.
2. **Financial Precision**: All monetary values must be stored in exact minor integer units (e.g. paise/cents) or exact SQL `NUMERIC`. Floating-point types (`FLOAT`, `DOUBLE`) are strictly forbidden for currency.
3. **Shift Payment Rules (`shift_payment_rules`)**: Configures rates/penalties for staff/managers attaching shift templates with attendance types (`PRESENT`, `LATE`, `HALF_DAY`, `ABSENT`).
4. **Unworked Payment Rules (`unworked_payment_rules`)**: Configures holiday, weekly off-day, and paid-leave payment amounts/multipliers (`HOLIDAY`, `WEEKLY_OFF`, `PAID_LEAVE`). Worked holidays and worked off-days are paid as normal present days; no separate worked-day rule types exist.
5. **Overtime Table (`overtime`)**: Overtime entries are created with individual payment amounts (`payment_cents`), status (`REQUESTED`, `APPROVED`, `COMPLETED`, `PAID`, `CLOSED`), dates, and planned/worked minutes. No separate overtime rules table needed.
6. **Monthly Salary Calculation & Payslip Engine**:
   - When a month completes, the backend calculates:
     - **Shift Earnings**: Sum of shift attendance pay based on `shift_payment_rules`.
     - **Holiday/Off-Day Pay**: Sum of holiday and weekly off-day pay based on `unworked_payment_rules`.
     - **Completed Overtime**: Sum of `payment_cents` for completed overtime records in `overtime`.
     - **Salary Slip**: Prepares `payroll_items` with gross salary, additions, deductions, net salary, and itemized breakdown.
7. **Double-Entry Wallet Ledger**:
   - User wallet balances are calculated views derived from append-only `wallet_transactions`.
   - Direct `UPDATE` statements on wallet balances are forbidden.
   - Every financial event (`ACCRUAL`, `CREDIT`, `DISBURSEMENT`, `DEDUCTION`, `SETTLEMENT`) requires an explicit transaction record, source reference, and idempotency key.

---

## 7. Attendance, GPS & Local Face Verification
1. **Premises Geofence**: Default radius of **50 metres**. Distance evaluated via geodesic FusedLocation readings.
2. **Clock-In Requirements**: Valid geofence check + local ONNX face verification match.
3. **Periodic GPS Monitoring**:
   - Active shift schedules trigger location checks approximately every 10 minutes.
   - Location checks are categorized (In-Premises, Outside, Stale, Low-Accuracy, Mocked, Offline).
   - **Three Consecutive Failures**: Automatically creates a location monitoring exception and sends real-time alerts to Manager and Staff.
   - **Rule**: Never automatically mark a Staff member absent due to GPS check failures. Requires manual Manager review.
4. **Local Biometrics**:
   - ONNX Runtime Mobile infers face embeddings on-device using CameraX input.
   - Only 128/512-dimensional numerical embeddings are transmitted/stored securely. Raw facial images are discarded after feature extraction.

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
- **NO** direct DB balance updates without ledger transaction entries.
- **NO** client-side salary or payroll calculation engine; backend is authoritative.
- **NO** shell-based file editing tools that bypass IDE file buffers.

---

## 10. Development Stages Overview
- **Stage 0**: Documentation, Architecture, ER Diagram, API Specifications.
- **Stage 1**: Project Setup & Infrastructure Initialization.
- **Stage 2**: Authentication, Invitations & Multi-Tenant Role Security (COMPLETED).
- **Stage 3**: Staff & Shift Management Module.
- **Stage 4**: On-Device ONNX Face Verification Module.
- **Stage 5**: Premises Geofencing & Periodic GPS Monitoring Engine.
- **Stage 6**: Attendance Calculation & Correction Resolution Engine.
- **Stage 7**: Salary Calculation Engine & Real-Time Wallet Ledger.
- **Stage 8**: Monthly Payroll Lifecycle & Payslip Engine.
- **Stage 9**: Real-time Notifications & Reporting Dashboards.
- **Stage 10**: Security Audit, E2E Verification & Release Preparation.
