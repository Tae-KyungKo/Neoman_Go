# Phase 8-5 Prod-like Docker

## 1. Conclusion

Phase 8-5 defines a local prod-like Docker environment for backend verification. It is not the production deployment topology.

The compose stack includes only:

- backend
- mysql
- redis

Frontend, Nginx, Certbot, CloudFront, S3, GitHub Actions, and ADMIN bootstrap are outside this step.

## 2. Files

- `Dockerfile`
- `.dockerignore`
- `docker-compose.prodlike.yml`
- `.env.prodlike.example`

The real `.env.prodlike` file must stay local and must not be committed.

## 3. Backend Image

The backend image uses a multi-stage build.

- Build stage: `gradle:8.14.3-jdk17`
- Runtime stage: `eclipse-temurin:17-jre-jammy`
- Runtime runs the Spring Boot jar with `java -jar /app/app.jar`
- Secrets are injected through environment variables at container runtime.
- Secret files such as `.env`, `.env.prodlike`, and `application-secret.yml` are excluded by `.dockerignore`.

The image does not bake production credentials.

## 4. Compose Services

### backend

- Exposes `8080:8080` for local verification.
- Uses `SPRING_PROFILES_ACTIVE=prodlike`.
- Uses the internal compose hostnames `mysql` and `redis`.
- Waits for MySQL and Redis healthchecks before starting.

### mysql

- Uses MySQL 8.
- Exposes `3307:3306` to avoid conflict with local MySQL.
- Uses database name `neomango_prodlike`.
- Uses `utf8mb4` and `utf8mb4_0900_ai_ci`.
- Uses a Docker named volume: `neomango_mysql_prodlike_data`.

### redis

- Does not expose a host port.
- Is reachable only inside the compose network.
- Requires `REDIS_PASSWORD`.
- Enables AOF with `appendfsync everysec`.
- Uses `maxmemory-policy noeviction`.
- Uses a Docker named volume: `neomango_redis_prodlike_data`.

## 5. How To Run

Create a local `.env.prodlike` from `.env.prodlike.example` and replace placeholders with local verification values.

Do not use production credentials.

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike up --build
```

The backend starts with the prodlike profile. On first boot, Flyway applies `V1__baseline_schema.sql`, then Hibernate validates the schema.

There is no Actuator health endpoint in this phase. Backend startup is verified by container logs and by confirming that the application starts without Flyway, Hibernate validate, or Redis connection errors.

## 6. Verification Targets

- Docker image build succeeds.
- MySQL container becomes healthy.
- Redis container becomes healthy.
- Backend container starts with `prodlike`.
- Flyway V1 migration succeeds.
- Hibernate `ddl-auto=validate` succeeds.
- Redis connection succeeds.

## 7. Cleanup

Stop containers:

```powershell
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike down
```

Remove containers and named volumes when a clean migration rehearsal is needed:

```powershell
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike down -v
```

`down -v` deletes the prod-like MySQL and Redis named volumes. Do not run it against any production compose project.

## 8. Next Step

Phase 8-6 should add and verify Nginx reverse proxy behavior, including CORS and SSE proxy settings. This step intentionally does not add Nginx configuration.

## 9. Verification Result

Verification date: 2026-06-12

Executed checks:

- `.\gradlew.bat compileJava`: success
- `.\gradlew.bat test`: success
- `docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike config --quiet`: success
- `docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike up --build -d`: success
- Backend HTTP check on `GET /api/notices`: HTTP 200

Runtime result:

- Backend image build succeeded.
- MySQL 8.4.9 container became healthy.
- Redis 7.4.9 container became healthy.
- Backend started with the `prodlike` profile.
- Flyway applied `V1__baseline_schema.sql`.
- `flyway_schema_history` contains version `1`, description `baseline schema`, success `1`.
- Hibernate `ddl-auto=validate` passed.
- Redis runtime config confirmed `appendonly yes`, `appendfsync everysec`, and `maxmemory-policy noeviction`.

Created tables:

- `audit_logs`
- `comments`
- `flyway_schema_history`
- `notices`
- `notifications`
- `posts`
- `team_applications`
- `team_members`
- `teams`
- `user_category_memberships`
- `users`

Observed warnings:

- Flyway still warns that MySQL 8.4 is newer than the latest MySQL version tested by the current Flyway version. Migration and validation passed, but production RDS MySQL minor version should be selected with Flyway support in mind.
- Spring Boot logs a generated security password because the current Security configuration still allows default user auto-configuration to initialize. This does not block Phase 8-5 Docker verification, but it should be reviewed before production hardening.
