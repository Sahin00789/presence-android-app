# Deployment, Infrastructure & Production Readiness (`docs/DEPLOYMENT.md`)

## 1. Environment Setup & Configuration

### 1.1 Docker Compose Configuration
The repository includes a production-ready `docker-compose.yml` for backend orchestration:
- `server`: Fastify Node.js application container.
- `postgres`: PostgreSQL 16 database container with persistent volume.
- `redis`: Redis 7 container with persistent appendonly file (`AOF`).

### 1.2 Environment Variables (.env)
Validated on server startup via Zod in `server/src/config/env.ts`:
```dotenv
NODE_ENV=production
HOST=0.0.0.0
PORT=3000

DATABASE_URL=postgresql://presence_user:secure_password@postgres:5432/presence_db?sslmode=prefer
REDIS_URL=redis://:redis_password@redis:6379

GOOGLE_WEB_CLIENT_ID=123456789-web.apps.googleusercontent.com
GOOGLE_WEB_CLIENT_SECRET=GOCSPX-secret_key
GOOGLE_REDIRECT_URI=https://api.presence.app/api/v1/auth/google/callback

ACCESS_TOKEN_SECRET=super_secret_jwt_key_min_32_chars
REFRESH_TOKEN_SECRET=super_secret_refresh_key_min_32_chars

CORS_ORIGIN=https://presence.app
```

---

## 2. Database Migrations & Seeding
1. **Generate Migrations**: `pnpm run db:generate` generates SQL migrations from the Drizzle schema.
2. **Apply Migrations**: `pnpm run db:migrate` runs `drizzle-kit migrate` against `DATABASE_URL`.
3. **Existing Databases**: Review generated SQL before applying it. The initial migration creates the application schema and is intended for a fresh database; do not apply it to a database that already contains these tables.
4. **Zero Downtime**: Schema changes use additive migrations (adding nullable columns or new tables first before deprecating old ones).
5. **Seeding**: Initial system seeds create permissions, default shift templates, and system configuration keys.

---

## 3. Android Build & GitHub Release Distribution
The project uses GitHub Releases for client APK distribution instead of Google Play Store.

### 3.1 APK Output Configuration
- **Gradle Task**: `./gradlew assembleRelease`
- **Output File**: `app/build/outputs/apk/release/presence.apk`
- **Filename Rule**: Configured in `app/build.gradle.kts` via `applicationVariants` to automatically set `outputFileName = "presence.apk"`.

### 3.2 Git Remote & Repository Setup
To push updates to the GitHub repository:
```bash
git remote add origin https://github.com/Sahin00789/presence-android-app.git
git branch -M main
git push -u origin main
```

### 3.3 GitHub Release Deployment Workflow
1. **Automated CI/CD Release (GitHub Actions)**:
   - On every push to `main` branch, `.github/workflows/android-release.yml` automatically compiles `presence.apk` and updates the `latest` GitHub Release tag.
   - Users can always download the latest APK directly from `https://github.com/Sahin00789/presence-android-app/releases`.
2. **Local PowerShell Release Script**:
   - Run `.\scripts\release-github.ps1` in PowerShell to assemble the release APK and publish/update the `latest` release on GitHub via `gh` CLI.

---

## 4. Monitoring, Health Checks & Backups
1. **Health Endpoints**:
   - `GET /health/live`: Liveness probe (returns 200 OK).
   - `GET /health/ready`: Readiness probe (verifies PostgreSQL and Redis connections).
2. **Redis Startup**: Redis connects lazily on the first Redis-dependent operation; a Redis outage does not prevent server startup.
3. **Development Logs**: Pino pretty output is compact and single-line. Server startup messages use ASCII for Windows console compatibility.
4. **Database Backups**: Daily automated `pg_dump` backups pushed to encrypted S3-compatible cloud storage with a 30-day retention policy.
5. **Disaster Recovery**: Tested restore workflow recovers database state within 15 minutes.
