# Phase 8-6 Nginx CORS SSE

## 1. Conclusion

Phase 8-6 adds a local prod-like Nginx reverse proxy in front of the backend.

This is a local verification topology, not the final production TLS topology. Production TLS, port 443, certbot, DNS cutover, GitHub Actions deployment, ADMIN bootstrap, and Actuator health hardening remain outside this phase.

## 2. Files

- `docker-compose.prodlike.yml`
- `nginx/prodlike/default.conf`
- `src/main/resources/application.yml`
- `src/main/java/com/neomango/global/config/SecurityConfig.java`

The real `.env.prodlike` file must stay local and must not be committed.

## 3. Nginx Compose Policy

The prod-like compose stack now includes `nginx`.

- Image: `nginx:1.27-alpine`
- Host port: `8081`
- Container port: `80`
- Config mount: `./nginx/prodlike/default.conf:/etc/nginx/conf.d/default.conf:ro`
- Upstream backend: `http://backend:8080`

Nginx depends on the backend container. There is no Actuator health endpoint in this phase, so `depends_on` does not prove application readiness. Runtime readiness is verified by HTTP checks through Nginx.

## 4. General API Proxy

General requests are proxied to the backend through `location /`.

Nginx forwards:

- `Host`
- `X-Real-IP`
- `X-Forwarded-For`
- `X-Forwarded-Proto`
- `Authorization`

Authorization forwarding is explicit so JWT Bearer tokens continue to be validated by the Spring Security filter.

## 5. SSE Proxy

`/api/notifications/stream` has a dedicated location because SSE is long-lived streaming traffic.

Applied Nginx settings:

- `proxy_buffering off`
- `proxy_cache off`
- `proxy_read_timeout 3600s`
- `proxy_send_timeout 3600s`
- `proxy_http_version 1.1`
- `proxy_set_header Connection ""`
- `add_header X-Accel-Buffering no always`

The backend controller still produces `text/event-stream`. If the request has no valid JWT, `401` is the expected result and still proves the request reaches the authenticated endpoint.

## 6. CORS Policy

Allowed origins:

- local: `http://localhost:5173`
- prod-like: `http://localhost:5173`, and `https://neomango.kr` only when explicitly needed for rehearsal
- prod: `https://neomango.kr`

`https://www.neomango.kr` is a redirect target to the root domain and is not included as a default allowed origin.

Credentials policy:

- `allowCredentials=false`
- Refresh Token is not an HttpOnly Cookie in the current backend contract.
- Login returns `accessToken` and `refreshToken` in JSON.
- Reissue receives `refreshToken` in the JSON request body.
- Access Token is read from `Authorization: Bearer`.

Allowed request headers:

- `Authorization`
- `Content-Type`
- `Accept`
- `Last-Event-ID`
- `Cache-Control`

Wildcard production CORS origins are not allowed.

## 7. SSE Authentication Policy

SSE authentication remains JWT Bearer header based.

- Do not pass the access token as a query parameter.
- Do not change the Refresh Token flow to Cookie or HttpOnly Cookie in this phase.
- Native `EventSource` alone does not fit the current authentication contract because it cannot attach an `Authorization` header.
- The frontend should use a fetch-based SSE client, or another SSE client that can attach `Authorization: Bearer`.

Frontend implementation still needs to be checked against this policy.

## 8. Heartbeat And Reconnect Policy

The backend sends an initial `connected` event, uses a one-hour `SseEmitter` timeout, and sends a 30-second SSE comment heartbeat.

Heartbeat is connection liveness traffic, not a business notification. Heartbeat success is not logged at INFO. Failed heartbeat sends remove only the failed connection.

Frontend reconnect policy:

- On access token expiry or SSE `401`, refresh the token and reconnect SSE.
- If refresh fails, route the user to login.
- After reconnect, query the REST notification list API to recover missed notifications.

Server-side `Last-Event-ID` replay, Outbox, Redis Pub/Sub, and broker-based fan-out are not implemented in Phase 8-7.

## 9. Generated Security Password Warning

Cause: Spring Boot creates a default in-memory user when Spring Security is present and no custom `UserDetailsService`, `AuthenticationProvider`, or `AuthenticationManager` bean exists.

Decision: The backend uses stateless JWT authentication, not form login or HTTP Basic. Phase 8-6 disables form login and HTTP Basic and defines a minimal `UserDetailsService` that always fails lookup. This prevents the default user auto-configuration from exposing a generated development user.

Actuator security hardening remains a Phase 8-10 task.

## 10. Verification Commands

Compile and test:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
```

Compose config and startup:

```powershell
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike config --quiet
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike up --build -d
```

Nginx API check:

```powershell
curl.exe -i http://localhost:8081/api/notices
```

CORS preflight check:

```powershell
curl.exe -i -X OPTIONS http://localhost:8081/api/notices `
  -H "Origin: http://localhost:5173" `
  -H "Access-Control-Request-Method: GET" `
  -H "Access-Control-Request-Headers: Authorization, Content-Type"
```

Authorization forwarding check with a deliberately invalid token:

```powershell
curl.exe -i http://localhost:8081/api/notifications `
  -H "Authorization: Bearer invalid-token-for-forwarding-check"
```

SSE unauthenticated check:

```powershell
curl.exe -i -N http://localhost:8081/api/notifications/stream
```

SSE authenticated header check requires a local verification account. Do not print token values in logs or reports.

## 11. Cleanup

Stop containers while preserving named volumes:

```powershell
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike down
```

Remove named volumes only when a clean local migration rehearsal is explicitly needed:

```powershell
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike down -v
```

`down -v` deletes prod-like MySQL and Redis data. Do not use it against production.

## 12. Phase 8-7 Follow-up

- Confirm the frontend uses a fetch-based SSE client with `Authorization` header support.
- Implement the frontend token refresh and reconnect flow.
- Re-test authenticated SSE through Nginx with a local verification account without printing token values.

## 13. Phase 8-10 Actuator Proxy Update

The existing Nginx `location /` proxy allows access to:

- `GET /actuator/health`
- `GET /actuator/info`

Spring Actuator web exposure is limited to health/info, so sensitive actuator endpoints such as env, beans, metrics, loggers, heapdump, and threaddump are not exposed through Nginx. SSE proxy settings and token query parameter prohibition remain unchanged.
