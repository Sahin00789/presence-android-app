# Development Stages & Completion Criteria (`docs/DEVELOPMENT_STAGES.md`)

This document outlines the sequential implementation stages for building the Staff Attendance, Shift Management & Salary Wallet System (`presence`).

---

## Stage Roadmap Overview

### Stage 0: Documentation & System Architecture (COMPLETED)
- [x] Inspect existing repository structure.
- [x] Create `AGENTS.md` defining project rules, architecture, and coding standards.
- [x] Create `README.md` with overview, architecture diagrams, and quickstart guide.
- [x] Create detailed architecture specification (`docs/ARCHITECTURE.md`).
- [x] Create system requirements & SLA specification (`docs/REQUIREMENTS.md`).
- [x] Create role permissions matrix & invitation workflows (`docs/ROLE_PERMISSIONS.md`).
- [x] Create Google OAuth authorization-code exchange architecture (`docs/AUTHENTICATION.md`).
- [x] Create PostgreSQL database schema & Mermaid ER diagram (`docs/DATABASE.md`).
- [x] Create REST API v1 specification (`docs/API.md`).
- [x] Create security, encryption & privacy guidelines (`docs/SECURITY.md`).
- [x] Create attendance state machine & rules engine (`docs/ATTENDANCE_RULES.md`).
- [x] Create local ONNX biometric face verification policy (`docs/FACE_VERIFICATION.md`).
- [x] Create geofencing & ~10-min active shift GPS monitoring specification (`docs/LOCATION_MONITORING.md`).
- [x] Create shift template & overnight shift rules (`docs/SHIFT_RULES.md`).
- [x] Create salary calculation engine formulas & compensation models (`docs/SALARY_RULES.md`).
- [x] Create real-time wallet double-entry ledger architecture (`docs/WALLET_LEDGER.md`).
- [x] Create monthly payroll lifecycle & payslip engine (`docs/PAYROLL.md`).
- [x] Create automated testing strategy & financial invariant suite (`docs/TESTING.md`).
- [x] Create deployment, Docker & production readiness guide (`docs/DEPLOYMENT.md`).
- [x] Create development roadmap & completion checklists (`docs/DEVELOPMENT_STAGES.md`).
- **Exit Criteria**: All Stage 0 documentation files created, validated, and aligned with system prompt requirements.

---

### Stage 1: Project Setup & Infrastructure Initialization (COMPLETED)
- [x] Configure Android Gradle dependencies catalog (`gradle/libs.versions.toml`) with Hilt, Retrofit, Room, DataStore, CameraX, ONNX Runtime Mobile, Fused Location, and WorkManager.
- [x] Initialize Fastify 5 TypeScript backend in `/server` with Zod, Drizzle ORM, `postgres`, and `ioredis`.
- [x] Set up PostgreSQL 16 and Redis 7 Docker Compose dev environment (`docker-compose.yml`).
- [x] Implement startup Zod environment variable validation (`server/src/config/env.ts`).
- [x] Implement complete Drizzle ORM database schema (`server/src/db/schema.ts`).
- [x] Configure Drizzle configuration (`drizzle.config.ts`).
- [x] Add Fastify infrastructure plugins (`postgres.ts`, `redis.ts`, `authentication.ts`, `authorization.ts`).
- [x] Add structured error handling (`app-error.ts`) and Zod type provider integration.
- [x] Implement system health endpoints (`/health/live`, `/health/ready`).
- [x] Create backend integration test suite (`tests/health.test.ts`).
- **Exit Criteria**: Android client setup complete; Fastify TypeScript server architecture initialized with DB schemas, Redis, Auth plugins, and health routes.

---

### Stage 2: Authentication, Invitations & Multi-Tenant Role Security (COMPLETED)
- [x] Implement Google authorization-code exchange and email OTP authentication.
- [x] Implement JWT access token issuance, rotating refresh tokens, and session tracking.
- [x] Implement Owner identity verification and organization registration.
- [x] Implement Manager and Staff invitation flows.
- [x] Implement authentication and organization authorization guards.
- [x] Build Android authentication screens with role selection, Google Sign-In, email OTP, and token storage.
- **Exit Criteria**: Authentication, token rotation, invitation acceptances, and multi-tenant isolation unit tests pass.

---

### Stage 3: Staff Management, Premises & Shift Scheduling Module (COMPLETED)
- [x] Implement Staff profile creation, employment records, and salary structure configuration endpoints.
- [x] Implement Premises configuration endpoint (latitude, longitude, geofence radius).
- [x] Implement Shift template creation and shift assignment endpoints.
- [x] Implement Weekly-off schedule and Public Holiday calendar management.
- [x] Build Android Manager screens for Staff listing, Shift scheduling, and Premises configuration.
- [x] Build Android Staff screen for viewing assigned shift schedules.
- **Exit Criteria**: Shift scheduling, overnight shift assignments, and premises configuration pass integration tests.

---

### Stage 4: On-Device ONNX Face Verification Module (IMPLEMENTED)
- [ ] Download and verify lightweight MobileFaceNet / ArcFace ONNX model (~3.5MB) with permissive license.
- [x] Implement Android CameraX frame capture and quality preprocessing (blur, lighting, head pose).
- [x] Integrate ONNX Runtime Mobile for on-device 128/512-dim face embedding extraction.
- [x] Implement **Owner/Manager-supervised** face enrollment with encrypted backend embedding storage.
- [x] Implement server-side clock-in face verification against the enrolled template ($\ge 0.75$ cosine similarity).
- [x] Implement manual clock-in fallback request workflow.
- **Exit Criteria**: On-device face inference passes real-time testing and handles lighting/blur edge cases correctly.
- **Production note**: `app/src/main/assets/face_model.onnx` is not bundled in this repository. Debug builds use a luminance fallback embedding; release builds refuse to start face capture until the model is provisioned. See `docs/FACE_VERIFICATION.md`.

---

### Stage 5: Premises Geofencing & Periodic GPS Monitoring Engine (COMPLETED)
- [x] Implement spherical geodesic Haversine distance calculation and geofence validation on Android & backend.
- [x] Implement Android Fused Location Provider integration and 30m accuracy threshold check.
- [x] Build Android Foreground Service & WorkManager fallback for ~10-minute active-shift location checks.
- [x] Implement periodic location check backend endpoint (`POST /api/v1/location/checks`).
- [x] Implement the 3-consecutive-failures rule: generates location exception and alerts Manager + Staff.
- [x] Implement offline location check queuing in Room DB with background synchronization upon reconnection.
- **Exit Criteria**: Periodic GPS checks execute reliably during active shifts, trigger 3-failure alerts, and recover gracefully.

---

### Stage 6: Attendance Calculation & Exception Resolution Engine (COMPLETED)
- [x] Implement full attendance state machine (Scheduled, Present, Late, Half-Day, Absent, Leave, Holiday, Worked Off-Day).
- [x] Implement late grace period and half-day working duration calculation logic.
- [x] Implement leave request submission, Manager approval, and holiday schedule overrides.
- [x] Implement attendance correction workflow for resolving location/face exceptions with immutable audit history.
- [x] Build Android Manager Attendance Exception Review & Correction screen.
- [x] Build Android Staff Attendance History screen.
- **Exit Criteria**: Attendance state transitions pass automated tests and preserve immutable event history.

---

### Stage 7: Salary Calculation Engine & Real-Time Wallet Ledger (COMPLETED)
- [x] Implement centralized backend salary calculation engine for 5 compensation models (Monthly, Daily, Hourly, Per-Shift, Mixed).
- [x] Implement effective-dated shift payment rules (`shift_payment_rules`) and unworked payment rules (`unworked_payment_rules`) for holidays, weekly off-days and paid leave using integer minor units (`bigint`).
- [x] Implement double-entry append-only ledger transaction posting (`wallet_transactions`).
- [x] Implement real-time Redis Pub/Sub + Fastify SSE stream for broadcasting wallet balance updates to Android app.
- [x] Build Android Staff Real-Time Salary Wallet screen showing Earned, Approved Unpaid, Paid, Advances, and Transaction History.
- **Exit Criteria**: Wallet ledger reconciliation invariant tests pass; zero floating-point accumulation errors across synthetic test cycles.

---

### Stage 8: Monthly Payroll Lifecycle & Payslip Engine (COMPLETED)
- [x] Implement monthly payroll state machine (Draft -> Calculating -> Review -> Approval -> Locked -> Paid -> Closed).
- [x] Implement automatic exception block preventing payroll review until open exceptions are resolved.
- [x] Implement Owner payroll approval & locking endpoint.
- [x] Implement manual payment disbursement recording (`payment_records`).
- [x] Implement itemized payslip generation endpoint and snapshot preservation.
- [x] Build Android Manager Payroll Preparation & Payment Recording screens.
- [x] Build Android Owner Payroll Review & Approval screens.
- [x] Build Android Staff Payslip View screen.
- **Exit Criteria**: Payroll locking freezes attendance records; duplicate payment disbursements are prevented.

---

### Stage 9: Real-Time Notifications & Reporting Dashboards (COMPLETED)
- [x] Implement the in-app notification engine for shift changes, 3-failure GPS alerts, leave approvals, and payroll events.
- [x] Implement in-app SSE notification delivery with Redis deduplication and Android Room persistence.
- [x] Build polished Android Role Dashboards:
  - Staff Dashboard (Today's shift, clock-in/out, GPS check status, earned wallet summary, recent transactions).
  - Manager Dashboard (Live staff present/late/absent count, GPS exceptions, leave requests, payroll progress).
  - Owner Dashboard (Org overview, financial liabilities, wallet balances, Manager activity, operational audit log).
- [x] Implement attendance, payroll, wallet and audit reporting endpoints with JSON/CSV/PDF exports.
- **Exit Criteria**: Role dashboards display live server data accurately; notification deduplication verified.
- **Optional production extension**: OS-level push notifications (FCM) are not implemented; real-time delivery uses the authenticated SSE stream and local Room persistence.

---

### Stage 10: Security Audit, E2E Verification & Release Preparation
- [ ] Execute security audit: OAuth code security, multi-tenant isolation, rate limiting, and biometrics privacy.
- [ ] Execute financial invariant regression suite and stress test database transaction isolation.
- [ ] Verify battery consumption of active-shift GPS monitoring on real Android test devices ($< 3\%$ per 8 hours).
- [ ] Verify ProGuard / R8 rules for release APK build.
- [ ] Complete E2E walk-through of all Owner, Manager, and Staff user journeys.
- **Exit Criteria**: All automated test suites green; release APK built and verified; documentation fully updated.
