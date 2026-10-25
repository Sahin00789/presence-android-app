# Security, Encryption & Privacy Guidelines (`docs/SECURITY.md`)

## 1. Network & Auth Security
1. **TLS 1.3 / HTTPS**: Mandatory for all client-server communications.
2. **Client Secret Protection**: Web Client Secrets for Google OAuth reside **exclusively** in backend `.env` files. The Android APK must never contain backend client secrets.
3. **Session Hardening**: Access tokens are short-lived (15 min). Refresh tokens use token rotation and reuse detection.

---

## 2. Biometric Data Protection & Privacy
1. **On-Device Feature Extraction**: Facial feature extraction occurs locally on the Android device using ONNX Runtime Mobile.
2. **Raw Image Destruction**: Raw bitmap face captures are held in memory only during model execution and are immediately purged. Raw facial images are **never** uploaded to servers.
3. **Embedding Storage**: 128/512-dim face embeddings stored in PostgreSQL `face_templates` are encrypted using AES-256-GCM column encryption.
4. **Biometric Consent & Deletion**: Employees must explicitly consent before face enrollment. Upon employment termination, face embedding records are deleted in accordance with privacy policies.

---

## 3. Location & GPS Data Privacy
1. **Foreground Service Disclosure**: Active-shift location checks run via a visible Android Foreground Service with explicit notification prompts.
2. **Shift-Bound Tracking**: Location checks are executed **only** during active shift schedules. No tracking occurs outside assigned shifts.
3. **Location Retention**: Historical location check pings are anonymized/purged after 90 days; only aggregated attendance summaries are retained indefinitely.

---

## 4. Multi-Tenant Data Isolation & Rate Limiting
1. **Row-Level Organization Guard**: Every API route enforces `organization_id` matching against the verified JWT payload.
2. **Rate Limiting**: Configured in Redis using sliding window token buckets:
   - Auth endpoints: 5 requests per minute per IP.
   - Clock-in / Location checks: 30 requests per minute per user.
   - General API endpoints: 100 requests per minute per user.
3. **Audit Logging**: All write operations, attendance corrections, and payroll approvals write an entry to `audit_logs`.
