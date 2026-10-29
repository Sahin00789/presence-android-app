# System Architecture (`docs/ARCHITECTURE.md`)

## 1. High-Level Architecture
`presence` follows a multi-tenant client-server architecture:
- **Android Client (`/app`)**: Jetpack Compose (Material 3), MVVM Clean Architecture, Hilt DI, Retrofit, Room DB, ONNX Runtime Mobile for local face verification.
- **Backend Server (`/server`)**: Node.js LTS, Fastify 5, TypeScript, PostgreSQL 16+ with Drizzle ORM, WebSockets (`/api/v1/events/ws`) for live event streaming.

---

## 2. Core Modules & Data Flow
- **Authentication**: JWT access tokens (15-min) and refresh tokens with single-device enforcement (`logged_devices`).
- **Staff & Shifts**: Managed via `employees`, `shifts`, and `employee_shift_assignments`.
- **Attendance & GPS**: Verified viapremises geofencing, ONNX face templates (`multi_pose_face_templates`), and periodic GPS pings (`attendance_location_pings`).
- **Earnings & Payroll**: Append-only earning ledger (`staff_earning_ledger`) and monthly payroll periods (`monthly_payrolls`, `payroll_transactions`).
- **Data Flow**: `Compose UI` -> `ViewModel` -> `UseCase` -> `Repository` -> `DataSource` -> `Fastify API` -> `PostgreSQL`.
