# Production Runbook

## Scope

This runbook covers first-phase backend operations for EC2 + Docker + Nginx, RDS MySQL, and Docker Redis.

Do not print or paste production secrets, tokens, Authorization headers, DB passwords, Redis passwords, JWT secrets, AWS credentials, or ADMIN passwords.

## Daily Status Checks

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail=200 backend
docker compose -f docker-compose.prod.yml logs --tail=200 redis
sudo systemctl status nginx --no-pager
sudo nginx -t
curl -i https://api.neomango.kr/actuator/health
```

Expected:

- backend container is healthy
- redis container is healthy
- Nginx is active
- `/actuator/health` returns HTTP 200 and `{"status":"UP"}`

## Log Locations

Backend:

```bash
docker logs <backend-container>
docker compose -f docker-compose.prod.yml logs backend
```

Redis:

```bash
docker compose -f docker-compose.prod.yml logs redis
```

Nginx:

```bash
sudo tail -n 200 /var/log/nginx/access.log
sudo tail -n 200 /var/log/nginx/error.log
```

Do not add request body logging or Authorization header logging during incident response.

## Redis Checks

Redis runs inside Docker and must not expose a public host port.

Check container:

```bash
docker compose -f docker-compose.prod.yml ps redis
```

Check Redis from inside the container without printing password values:

```bash
docker compose -f docker-compose.prod.yml exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD" ping'
docker compose -f docker-compose.prod.yml exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD" config get appendonly'
docker compose -f docker-compose.prod.yml exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD" config get appendfsync'
docker compose -f docker-compose.prod.yml exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD" config get maxmemory-policy'
```

Expected:

- `PONG`
- `appendonly yes`
- `appendfsync everysec`
- `maxmemory-policy noeviction`

## RDS Connectivity Failure

Symptoms:

- `/actuator/health` returns DOWN or non-200
- backend logs show DB connection failure
- API requests fail with server errors

Procedure:

1. Check RDS status in AWS console.
2. Check backend EC2 security group outbound.
3. Check RDS security group inbound from backend EC2 security group.
4. Check RDS endpoint and port in `.env.prod` on the server without copying values into reports.
5. Check recent Flyway and Hibernate validate logs.
6. If migration caused failure, follow rollback guide and snapshot policy.

## SSE Failure

Symptoms:

- frontend does not receive realtime notifications
- `/api/notifications/stream` disconnects
- Nginx timeout errors

Procedure:

1. Confirm normal API auth works.
2. Confirm frontend sends `Authorization: Bearer` header.
3. Do not use access tokens in query parameters.
4. Check Nginx SSE location settings:
   - `proxy_buffering off`
   - `proxy_read_timeout 3600s`
   - `proxy_send_timeout 3600s`
5. Check backend logs for SSE WARN logs.
6. Use REST notification list API to recover missed notifications.

## CORS Failure

Symptoms:

- browser blocks API calls from frontend
- preflight fails

Procedure:

1. Confirm frontend origin is `https://neomango.kr`.
2. Confirm backend `CORS_ALLOWED_ORIGINS` includes only intended production origin.
3. Check preflight:

```bash
curl -i -X OPTIONS https://api.neomango.kr/api/notices \
  -H "Origin: https://neomango.kr" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization, Content-Type"
```

Do not widen CORS to wildcard in production.

## Nginx Checks

```bash
sudo nginx -t
sudo systemctl status nginx --no-pager
sudo systemctl reload nginx
```

Use reload instead of restart when possible to avoid unnecessary connection drops.

## Sensitive Data Policy

Never log or paste:

- Authorization header
- access token
- refresh token
- JWT secret
- DB password
- Redis password
- AWS credential
- ADMIN password
- user password

If a secret is exposed, rotate it and document the incident without repeating the value.
