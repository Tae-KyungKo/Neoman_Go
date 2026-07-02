# Release Checklist

## Branch And Tag

- [ ] `dev` contains all approved Phase 8 changes.
- [ ] Backend CI passed.
- [ ] Frontend CI passed in the frontend repository.
- [ ] `release/v1.0` branch created from `dev`.
- [ ] Prod-like rehearsal completed on release branch.
- [ ] `release/v1.0` merged to `main`.
- [ ] `v1.0.0` tag created from `main`.
- [ ] Production deploy uses the immutable tag.

## Secrets

- [ ] `.env.prod` is not committed.
- [ ] `.env.prod.example` contains placeholders only.
- [ ] `.env.prodlike` is not committed.
- [ ] No real DB password, Redis password, JWT secret, ADMIN password, AWS credential, or token exists in committed docs/code.
- [ ] ADMIN bootstrap password is prepared only for the first controlled run.

## Database

- [ ] Flyway migration list reviewed.
- [ ] No Flyway ADMIN seed data.
- [ ] Production uses Hibernate `ddl-auto=validate`.
- [ ] RDS backup or snapshot verified before deployment.

## Redis

- [ ] Redis port `6379` is not public.
- [ ] Redis password is configured in production environment only.
- [ ] AOF enabled.
- [ ] `appendfsync everysec`.
- [ ] `maxmemory-policy noeviction`.

## Domains And TLS

- [ ] Route 53 hosted zone ready.
- [ ] Registrar name servers changed to Route 53 name servers.
- [ ] `api.neomango.kr` record ready.
- [ ] `neomango.kr` frontend record ready.
- [ ] `www.neomango.kr` redirects to `neomango.kr`.
- [ ] TLS certificate issued and installed.

## Backend Smoke Tests

- [ ] `GET /actuator/health` returns HTTP 200 and `{"status":"UP"}`.
- [ ] `GET /actuator/info` returns HTTP 200.
- [ ] `/actuator/env`, `/beans`, `/metrics` do not expose content.
- [ ] `GET /api/notices` returns expected response.
- [ ] Login succeeds.
- [ ] Authenticated notification list succeeds.
- [ ] Unauthenticated SSE returns `401`.
- [ ] Authenticated SSE returns `text/event-stream`.

## ADMIN Bootstrap

- [ ] `ADMIN_BOOTSTRAP_ENABLED=false` by default.
- [ ] Enable bootstrap only for first ADMIN creation.
- [ ] Disable bootstrap after success.
- [ ] Remove `ADMIN_BOOTSTRAP_PASSWORD` after success.
- [ ] Confirm normal signup cannot create ADMIN.

## Phase 9-15 Prodlike QA - 2026-07-02

- [x] Docker stack restarted with `docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike down` and `up -d --build`.
- [x] Container status checked: `mysql` healthy, `redis` healthy, `backend` healthy, `nginx` running.
- [x] Health check passed directly on `http://localhost:8080/actuator/health` and through nginx on `http://localhost:8081/actuator/health`.
- [x] Flyway history verified: V1 `baseline schema` success, V2 `add login id to users` success.
- [x] `users.login_id` column exists with `uk_users_login_id`.
- [x] `uk_users_nickname` exists.
- [x] `login_id` and `nickname` use `utf8mb4_0900_as_cs` collation.
- [x] Redis connection verified with `PONG`.
- [x] Auth smoke passed: loginId/nickname availability, signup, loginId login, email-login validation failure, reissue, logout.
- [x] Refresh Token Redis key policy verified: keys use `refresh:{userId}`; loginId/email based keys were not created; logout removed the owner user's refresh key.
- [x] Notification REST/SSE smoke passed: authenticated stream returned `text/event-stream`, `connected` event, and `notification` event; REST list returned the same notification with `+09:00` createdAt.
- [x] Post validation passed: title 0 failed, title 100 succeeded, title 101 failed, content 0 failed, content 5000 succeeded, content 5001 failed.
- [x] Comment validation passed: content 0 failed, content 1000 succeeded, content 1001 failed.
- [x] `ADMIN_BOOTSTRAP_ENABLED=false` behavior confirmed from backend log: `ADMIN bootstrap is disabled.`
- [x] Verification commands passed: `.\gradlew.bat compileJava`, `.\gradlew.bat test`, `git diff --check`.
- [x] Scope guard confirmed: `.env.prod` was not edited, no Flyway SQL was added, DB schema was not manually changed, Auth/JWT/RefreshToken/ADMIN/SSE policy was not changed.

Notes:

- Backend log had no fatal `ERROR`. Observed warnings were non-blocking: Flyway support warning for MySQL 8.4, Hibernate dialect deprecation, and Spring Page serialization warning.
- QA data was created in the prodlike local database: users `qauser01` and `qauser02`, team `QA Phase9 Team`, related application, posts, comments, and notification.
