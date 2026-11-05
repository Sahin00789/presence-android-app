# Staff Attendance, Shift Management & Salary Ledger System (`presence`)

Production-ready, multi-tenant mobile and backend application for staff attendance tracking, facial verification, GPS geofencing, periodic active-shift location monitoring, shift management, real-time salary ledger accounting, and monthly payroll processing.

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
                          │     Salary Calculation Engine + Ledger Service         │
                          └──────────────┬──────────────────────────┬──────────────┘
                                         │                          │
                                         ▼                          ▼
                          ┌──────────────────────────┐  ┌──────────────────────────┐
                          │   PostgreSQL 16 Database │  │   Memory / Cache         │
                          │   Auth Data, Ledgers,    │  │ Rate Limits, Sessions,   │
                          │   Shifts, Attendance, DB │  │ Live Event Coordination  │
                          └──────────────────────────┘  └──────────────────────────┘
```

---

## Role Summary

| Role | Key Capabilities |
| :--- | :--- |
| **Owner** | Head of the organization (`organizations.owner_user_id`). Manages everything across the organization and all assigned premises: self-registers after verifying identity with Google or email OTP; creates organization & configures all premises; directly adds Employees to premises; views organization-wide reports & workforce; approves financial adjustments and locks monthly payroll (`monthly_payrolls`). **The Owner is NOT an employee, has NO attendance tracking, and does NOT earn an employee salary.** |
| **Employee** | Premises-level workforce directly added by the Owner (`premises_id`). Authenticates via Google or email OTP; enrolled in person by Owner; performs face + GPS clock-in/out; undergoes ~10-min active-shift GPS checks; tracks shift schedules and attendance. **Employees ARE employees (`role = 'EMPLOYEE'`) who earn salary through payroll and access itemized monthly payslips.** |

---

## Key Features

1. **Google-first authentication**: Google Sign-In is primary, with email OTP as an alternative for each role (`OWNER`, `EMPLOYEE`). The login screen shows Google-specific privacy information only with Google sign-in; email OTP shows a separate note explaining that the code verifies the email and does not access Google data. Google is used for basic identity/profile verification only; Gmail, Drive, and contacts access are not requested. OAuth client secrets remain on the backend.
2. **Owner registration**: A three-step flow verifies identity with Google or email OTP, collects organization details, then configures and reviews the first premises. The privacy note changes with the selected verification method.
3. **On-Device ONNX Biometrics**: Local face embedding extraction using CameraX & ONNX Runtime Mobile, with supervised enrollment by an Owner and server-side similarity verification. Only 128/512-dim encrypted embeddings are stored; raw images never leave the device.
4. **50m Premises Geofence & Periodic GPS Checks**: GPS-based attendance with active-shift monitoring every ~10 minutes. 3 consecutive failures generate an alert without auto-marking absent.
5. **Real-Time salary ledger**: Append-only double-entry ledger accounting system tracking Earned Salary and Disbursements.
6. **Monthly Payroll Engine**: Complete lifecycle (`DRAFT` -> `CALCULATING` -> `REVIEW` -> `APPROVAL` -> `LOCKED` -> `PAID` -> `CLOSED`) with itemized payslip generation.

---

## Technology Stack

- **Android Client**: Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture, Coroutines/Flow, Hilt, Retrofit 2, Room DB, CameraX, ONNX Runtime Mobile, Fused Location Provider, WorkManager.
- **Backend Server**: Node.js LTS, TypeScript (Strict), Fastify 5, Drizzle ORM, PostgreSQL 16 (`postgres` client), Zod.

---

## Download & GitHub Release Distribution

The app is distributed via **GitHub Releases** (Play Store is not used).

- **Download APK**: Get the latest build of [`presence.apk`](https://github.com/Sahin00789/presence-android-app/releases/latest/download/presence.apk) from the [GitHub Releases Page](https://github.com/Sahin00789/presence-android-app/releases).
- **Build APK locally**:
  ```bash
  ./gradlew assembleRelease
  # Output: app/build/outputs/apk/release/presence.apk
  ```

### Quick Setup & Push to GitHub

```bash
git remote add origin https://github.com/Sahin00789/presence-android-app.git
git branch -M main
git push -u origin main
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
- [Ledger Integrity](./docs/ledger_LEDGER.md)
- [Monthly Payroll Lifecycle](./docs/PAYROLL.md)
- [Development Roadmap](./docs/DEVELOPMENT_STAGES.md)
