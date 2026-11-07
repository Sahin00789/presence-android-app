# Testing Strategy (`docs/TESTING.md`)

## 1. Testing Layers
- **Android Unit Tests**: JUnit + Coroutines Test + Truth for ViewModels, UseCases, and Repositories.
- **Backend Unit & Integration Tests**: Vitest + Supertest for Fastify API routes, authentication, attendance state machine, earning ledger calculations, and monthly payroll periods.
- **Biometric & Geo Tests**: Verification test suites for ONNX face embedding distance calculations and Haversine geofence calculations.

## 2. Verification Commands
- **Android Build & Test**: `./gradlew test assembleDebug`
- **Backend Test**: `cd server && npm run test`
- **Backend Typecheck**: `cd server && npm run typecheck`
