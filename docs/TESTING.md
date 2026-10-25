# Automated Testing Strategy & Financial Invariants (`docs/TESTING.md`)

## 1. Test Architecture Overview

The system enforces automated testing across Android, Fastify API, and PostgreSQL database layers.

```
+-----------------------------------------------------------------------+
|                      Automated Test Suite                            |
+------------------------------------+----------------------------------+
|          Android Tests             |          Backend Tests           |
|  - ViewModel & State Flow Tests    |  - Fastify Route Integration     |
|  - Room DAO & DataStore Unit Tests |  - Service & Salary Engine Tests |
|  - Compose UI & Screenshot Tests   |  - Drizzle DB Migration Tests    |
|  - ONNX / Location Fake Harnesses  |  - Financial Invariant Suite     |
+------------------------------------+----------------------------------+
```

---

## 2. Backend Test Suite
- **Framework**: Vitest / Node Test Runner + Supertest.
- **Isolated DB**: Integration tests execute against a dedicated PostgreSQL test container/database with fresh migrations.
- **Mock Google OAuth**: OAuth exchange tests use mock Google server certificates and token issuers.

### 2.1 Critical Test Scenarios
1. **Google OAuth Security**: Test code reuse, expired authorization codes, missing OAuth state, invalid client secret.
2. **Multi-Tenant Guard**: Verify that user from Org A receiving 403 Forbidden when requesting resources belonging to Org B.
3. **Role Enforcement**: Verify that Staff cannot invoke Manager endpoints, and Manager cannot invoke Owner-only approval endpoints.
4. **Attendance Edge Cases**: 3 consecutive failed GPS checks trigger exception without auto-absent mark; geofence boundary distance tests.

---

## 3. Financial Invariants Test Suite

The financial engine executes a dedicated invariant test suite on every PR:

1. **Invariance 1: Ledger Reconciliation**
   $$\text{Calculated Wallet Balance} \equiv \sum \text{wallet\_transactions.amount\_cents}$$
2. **Invariance 2: Double-Posting Prevention**
   Submitting identical clock-in or salary accrual with the same idempotency key **must** return 200 OK without inserting duplicate ledger rows.
3. **Invariance 3: Payment Immutability**
   Recorded payments on a `LOCKED` payroll item cannot be altered or deleted. Modifications require explicit signed reversals.
4. **Invariance 4: Decimal Math Precision**
   Verifies zero floating-point accumulation errors across 100,000 synthetic transaction cycles.

---

## 4. Android Test Suite
- **Framework**: JUnit 4/5, Kotlin Coroutines Test, Turbine for StateFlow testing, MockK / Fake repositories.
- **ViewModel Testing**: Test state transitions for clock-in, wallet updates, shift schedule loading, and error states.
- **Biometrics & Location Fakes**: Fake CameraX frame provider and fake Fused Location provider for deterministic automated UI tests.
