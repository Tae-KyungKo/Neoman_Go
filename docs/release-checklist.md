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
