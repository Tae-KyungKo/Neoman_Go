# Phase 8 Release Rehearsal

## Purpose

Run this rehearsal on prod-like infrastructure before the first production deployment.

## Backend Rehearsal

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike config --quiet
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike up --build -d
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike ps
```

Expected:

- backend healthy
- MySQL healthy
- Redis healthy
- Nginx up
- Flyway migration succeeds
- Hibernate validate succeeds

## Redis Rehearsal

Confirm:

- AOF enabled
- `appendfsync everysec`
- `maxmemory-policy noeviction`
- Redis port is not exposed to host internet

## Nginx Rehearsal

Check through Nginx:

```powershell
curl.exe -i http://localhost:8081/api/notices
curl.exe -i http://localhost:8081/actuator/health
curl.exe -i http://localhost:8081/actuator/info
curl.exe -i http://localhost:8081/actuator/env
curl.exe -i http://localhost:8081/actuator/beans
curl.exe -i http://localhost:8081/actuator/metrics
curl.exe -i -N http://localhost:8081/api/notifications/stream
```

Expected:

- `/api/notices`: HTTP 200
- `/actuator/health`: HTTP 200 with `{"status":"UP"}`
- `/actuator/info`: HTTP 200
- forbidden actuator endpoints: 401, 403, or 404
- unauthenticated SSE: 401
- authenticated SSE: 200 `text/event-stream`

Do not print token values.

## ADMIN Bootstrap Rehearsal

Default:

- `ADMIN_BOOTSTRAP_ENABLED=false`

First production ADMIN creation is a separate controlled step. Do not run it casually in rehearsal with real credentials.

## CI Rehearsal

Confirm:

- backend GitHub Actions CI passed
- frontend GitHub Actions CI passed in frontend repository
- Docker build passed
- no GitHub Secrets are required for backend CI

## Release Decision

Proceed only when:

- prod-like rehearsal passed
- release checklist passed
- rollback guide reviewed
- RDS snapshot/backup plan is confirmed
- DNS/TLS plan is confirmed
