# Rollback Guide

## Scope

Production rollback is tag-based. Database rollback is not handled by destructive down migrations.

## Backend Tag Rollback

1. Identify the last known good tag.
2. On the production server:

```bash
cd neomango-backend
git fetch --tags
git checkout <last-known-good-tag>
docker compose -f docker-compose.prod.yml up --build -d
docker compose -f docker-compose.prod.yml ps
curl -i https://api.neomango.kr/actuator/health
```

3. Confirm API smoke tests.
4. Document the rollback tag and reason.

## Database Policy

Flyway down migrations are not the default rollback strategy.

Production DB rollback policy:

- take an RDS snapshot before production migration
- prefer forward fix migrations
- restore from snapshot only when data/state rollback is explicitly approved

Before deployment:

1. Confirm latest automated RDS backup.
2. Create or verify a manual snapshot for high-risk releases.
3. Record snapshot identifier in the private deployment record, not in public docs if it exposes sensitive naming.

## Redis Rollback

Redis stores refresh tokens in the first deployment.

If Redis data is lost:

- existing access tokens remain valid until expiry
- refresh/reissue can fail
- users may need to log in again

Do not expose Redis port publicly during rollback.

## Frontend Rollback

Frontend rollback should use the previous S3 artifact or previous release build upload, followed by CloudFront invalidation if needed.

Document:

- previous frontend artifact version
- CloudFront invalidation id
- smoke test results on `https://neomango.kr`

## Rollback Smoke Tests

After backend rollback:

1. `GET https://api.neomango.kr/actuator/health`
2. `GET https://api.neomango.kr/api/notices`
3. login
4. authenticated notification list
5. unauthenticated SSE returns `401`
6. authenticated SSE returns `text/event-stream`

Do not print tokens in rollback notes.
