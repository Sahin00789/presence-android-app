# Employee Attendance, Shift Management & Salary Ledger System (`presence`)

<div align="center">

  ### 📱 Presence Android Application

  Production-ready, multi-tenant mobile and backend application for employee attendance tracking, facial verification, GPS geofencing, periodic active-shift location monitoring, shift management, real-time salary ledger accounting, and monthly payroll processing.

  [![Download APK](https://img.shields.io/badge/Download-presence.apk-2463EB?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Sahin00789/presence-android-app/releases/latest/download/presence.apk)
  [![Latest Release](https://img.shields.io/github/v/release/Sahin00789/presence-android-app?style=for-the-badge&color=22C55E&label=Latest%20Release)](https://github.com/Sahin00789/presence-android-app/releases/latest)
  [![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=github-actions)](https://github.com/Sahin00789/presence-android-app/actions)

</div>

---

## ⚡ Quick APK Download & Release Info

- 📥 **Direct Download**: [**Download `presence.apk`**](https://github.com/Sahin00789/presence-android-app/releases/latest/download/presence.apk)
- 📦 **Release Channel**: Distributed exclusively via [GitHub Releases](https://github.com/Sahin00789/presence-android-app/releases).
- 🛠 **Local Build Command**:
  ```bash
  ./gradlew assembleRelease
  # Output file location: app/build/outputs/apk/release/presence.apk
  ```

---

## 🏗 Architecture Overview

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

## 👥 Role Summary & Capabilities

| Role | Key Capabilities |
| :--- | :--- |
| **Owner** | Head of the organization (`organizations.owner_user_id`). Manages everything across the organization and all assigned premises: self-registers after verifying identity with Google or email OTP; creates organization & configures all premises; directly adds Employees to premises; views organization-wide reports & workforce; approves financial adjustments and locks monthly payroll (`monthly_payrolls`). **The Owner is NOT an employee, has NO attendance tracking, and does NOT earn an employee salary.** |
| **Employee** | Premises-level workforce directly added by the Owner (`premises_id`). Authenticates via Google or email OTP; enrolled in person by Owner; performs face + GPS clock-in/out; undergoes ~10-min active-shift GPS checks; tracks shift schedules and attendance. **Employees ARE employees (`role = 'EMPLOYEE'`) who earn salary through payroll and access itemized monthly payslips.** |

---

## 🔄 Core Workflows

### 👑 Owner Workflow
1. **Account Registration & Organization Setup**:
   - Authenticates via Google OAuth 2.0 or Email 6-Digit OTP.
   - Completes organization creation wizard and configures primary premises (address, 50m geofence radius, location coordinates).
2. **Employee Onboarding & Enrollment**:
   - Adds employees directly to assigned premises.
   - Conducts on-device face template enrollment using CameraX & ONNX Runtime (128/512-dimensional embeddings).
3. **Shift Roster & Payment Rules Configuration**:
   - Defines shifts per weekday with start time, end time, present pay rate, half-day threshold, absent rate, and leave rate.
   - Assigns employees to shifts using the shift assignment modal.
   - Sets unworked payment rules for holidays, weekly off-days, and paid leaves.
4. **Workforce Oversight & Real-Time Monitoring**:
   - Views live workforce attendance and premises status across all premises.
   - Receives automatic exception alerts when an active-shift employee records >3 consecutive outside-geofence pings (~every 10 minutes).
   - Uses manual resolution action buttons (`ABSENT`, `HALF_DAY`, `LEAVE`, `RESUME`) before shift completion if needed.
5. **Ledger Approval & Monthly Payroll Locking**:
   - Reviews completed overtime sessions and earning ledger entries.
   - Executes monthly payroll locking (`DRAFT` -> `CALCULATING` -> `REVIEW` -> `APPROVAL` -> `LOCKED` -> `PAID` -> `CLOSED`).

---

### 👤 Employee Workflow
1. **Authentication & Session Login**:
   - Signs in using Email OTP or Google OAuth using the pre-registered email address provided by the Owner.
   - Enforces single-device concurrency rule (logging in on a new device automatically invalidates previous active sessions).
2. **On-Premises Face + GPS Clock-In**:
   - Opens the app within 50m of the assigned premises geofence.
   - Performs local ONNX face verification scan to clock in for scheduled shift or overtime.
3. **Active-Shift Periodic Location Check**:
   - WorkManager background service executes periodic location checks (~every 10 minutes) during active shift hours.
4. **Shift Completion & Clock-Out**:
   - Performs clock-out scan upon completing shift duties. Attendance status is finalized and committed to PostgreSQL authoritative DB.
5. **Earning Ledger & Monthly Payslip Tracking**:
   - Inspects itemized real-time earning ledger records (`SHIFT`, `HOLIDAY`, `OFF_DAY`, `OVERTIME`).
   - Tracks accrued monthly earnings and views itemized monthly payslips upon payroll lock.

---

## 🚀 Key Features

1. **Google-first authentication**: Google Sign-In is primary, with email OTP as an alternative for each role (`OWNER`, `EMPLOYEE`).
2. **Owner registration**: Step-by-step wizard to setup organization & configure premises.
3. **On-Device ONNX Biometrics**: Local face embedding extraction using CameraX & ONNX Runtime Mobile with zero raw image transmission.
4. **50m Premises Geofence & Periodic GPS Checks**: Geofence enforcement with ~10-minute active-shift monitoring.
5. **Real-Time Employee Earning Ledger**: Double-entry append-only ledger accounting.
6. **Monthly Payroll Engine**: Complete payroll locking lifecycle with payslip generation.

---

## 🛠 Technology Stack

- **Android Client**: Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture, Coroutines/Flow, Hilt, Retrofit 2, Room DB, CameraX, ONNX Runtime Mobile, Fused Location Provider, WorkManager.
- **Backend Server**: Node.js LTS, TypeScript (Strict), Fastify 5, Drizzle ORM, PostgreSQL 16 (`postgres` client), Zod.

---

## 📤 Push & Release Instructions (via SSH)

```bash
git remote set-url origin git@github.com:Sahin00789/presence-android-app.git
git add .
git commit -m "Update documentation and release assets"
git push origin main
```

---

## 📚 Documentation Index

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
