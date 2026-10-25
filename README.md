# Staff Attendance, Shift Management & Salary Wallet System (`presence`)

Production-ready, multi-tenant mobile and backend application for staff attendance tracking, facial verification, GPS geofencing, periodic active-shift location monitoring, shift management, real-time salary wallet ledger accounting, and monthly payroll processing.

---

## Architecture Overview

```
                          ┌────────────────────────────────────────────────────────┐
                          │                 Android Native Client                  │
                          │          Jetpack Compose + Material 3 + MVVM           │
                          │     CameraX + Local ONNX Runtime Face Verification    │
                          │     Fused Location Provider + WorkManager GPS Check    │
                          └───────────────────────────┬────────────────────────────┘
                                                      │ HTTPS / REST / SSE
                                                      ▼
                          ┌────────────────────────────────────────────────────────┐
                          │                 Fastify 5 TypeScript API               │
                          │        Zod Validation + Google OAuth 2.0 Auth          │
                          │     Salary Calculation Engine + Wallet Ledger Service  │
                          └──────────────┬──────────────────────────┬──────────────┘
                                         │                          │
                                         ▼                          ▼
                          ┌──────────────────────────┐  ┌──────────────────────────┐
                          │   PostgreSQL 16 Database │  │   Redis Cache & Pub/Sub  │
                          │   Auth Data, Ledgers,    │  │ Rate Limits, Sessions,   │
                          │   Shifts, Attendance, DB │  │ Live Event Coordination  │
                          └──────────────────────────┘  └──────────────────────────┘
```

---

## Role Summary

| Role | Key Capabilities |
| :--- | :--- |
| **Owner** | Self-registers after verifying identity with Google or email OTP; creates and configures organization & premises; invites/deactivates Managers; views organization-wide reports & wallet liabilities; approves sensitive financial/attendance adjustments. |
| **Manager** | Invited by Owner; creates & manages Staff; assigns shift schedules; reviews attendance exceptions; resolves manual corrections; manages leave & holiday records; prepares monthly payroll; records payments. |
| **Staff** | Invited by Manager; authenticates via Google or email OTP; is enrolled in person by a Manager; performs face + GPS clock-in/out; undergoes ~10-min active-shift GPS checks; tracks real-time salary wallet; views payslips. |

---

## Key Features

1. **Google-first authentication**: Google Sign-In is primary, with email OTP as an alternative for each role. The login screen shows Google-specific privacy information only with Google sign-in; email OTP shows a separate note explaining that the code verifies the email and does not access Google data. Google is used for basic identity/profile verification only; Gmail, Drive, and contacts access are not requested. OAuth client secrets remain on the backend.
2. **Owner registration**: A three-step flow verifies identity with Google or email OTP, collects organization details, then configures and reviews the first premises. The privacy note changes with the selected verification method.
3. **On-Device ONNX Biometrics**: Local face embedding extraction using CameraX & ONNX Runtime Mobile, with supervised enrollment by an Owner/Manager and server-side similarity verification. Only 128/512-dim encrypted embeddings are stored; raw images never leave the device.
4. **50m Premises Geofence & Periodic GPS Checks**: GPS-based attendance with active-shift monitoring every ~10 minutes. 3 consecutive failures generate an alert without auto-marking absent.
5. **Real-Time Salary Wallet**: Append-only double-entry ledger accounting system tracking Earned Salary, Approved Unpaid Salary, Disbursements, Advances, and Deductions.
6. **Monthly Payroll Engine**: Complete lifecycle (`DRAFT` -> `CALCULATING` -> `REVIEW` -> `APPROVAL` -> `LOCKED` -> `PAID` -> `CLOSED`) with itemized payslip generation.

---

## Technology Stack

- **Android Client**: Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture, Coroutines/Flow, Hilt, Retrofit 2, Room DB, CameraX, ONNX Runtime Mobile, Fused Location Provider, WorkManager.
- **Backend Server**: Node.js LTS, TypeScript (Strict), Fastify 5, Drizzle ORM, PostgreSQL 16 (`postgres` client), Redis (`ioredis`), Zod.

---

## Repository Structure

```text
presence/
├── AGENTS.md                   # System rules, coding standards, architecture constraints
├── README.md                   # Project overview & quickstart
├── docs/                       # Complete specification documentation
│   ├── ARCHITECTURE.md         # System architecture & data flow
│   ├── REQUIREMENTS.md         # Functional & non-functional requirements
│   ├── ROLE_PERMISSIONS.md     # Permission matrix & invitation flows
│   ├── AUTHENTICATION.md       # Google OAuth flow & session security
│   ├── DATABASE.md             # PostgreSQL schema & Mermaid ER diagram
│   ├── API.md                  # REST API v1 specification
│   ├── SECURITY.md             # Security, encryption & privacy rules
│   ├── ATTENDANCE_RULES.md     # Attendance state machine & rules
│   ├── FACE_VERIFICATION.md    # Local ONNX biometric processing
│   ├── LOCATION_MONITORING.md  # Geofencing & periodic GPS checks
│   ├── SHIFT_RULES.md          # Shift schedules & overnight rules
│   ├── SALARY_RULES.md         # Calculation engine & compensation models
│   ├── WALLET_LEDGER.md        # Double-entry ledger accounting
│   ├── PAYROLL.md              # Monthly payroll lifecycle & payslips
│   ├── TESTING.md              # Automated testing strategy
│   ├── DEPLOYMENT.md           # Docker, migration & release guide
│   └── DEVELOPMENT_STAGES.md   # Stage 0 to 10 roadmap & checklists
├── app/                        # Native Android application
└── server/                     # Fastify TypeScript backend API
```

---

## Documentation Index

Detailed specifications and domain logic rules are available in the [`docs/`](./docs/) directory:
- [Architecture & Tech Stack](./docs/ARCHITECTURE.md)
- [Requirements & SLA](./docs/REQUIREMENTS.md)
- [Role Permission Matrix](./docs/ROLE_PERMISSIONS.md)
- [Google OAuth & Security](./docs/AUTHENTICATION.md)
- [Database Schema & ER Diagram](./docs/DATABASE.md)
- [REST API Specifications](./docs/API.md)
- [Attendance State Engine](./docs/ATTENDANCE_RULES.md)
- [Face Verification Policy](./docs/FACE_VERIFICATION.md)
- [Location Monitoring & Geofencing](./docs/LOCATION_MONITORING.md)
- [Salary Calculation & Rules](./docs/SALARY_RULES.md)
- [Wallet Ledger Integrity](./docs/WALLET_LEDGER.md)
- [Monthly Payroll Lifecycle](./docs/PAYROLL.md)
- [Development Roadmap](./docs/DEVELOPMENT_STAGES.md)
