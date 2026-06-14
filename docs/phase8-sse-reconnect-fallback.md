# Phase 8-7 SSE Reconnect, DB Fallback, Logging

## 1. Conclusion

Phase 8-7 keeps Notification DB storage as the source of truth and treats SSE as a realtime helper channel.

If an SSE connection is missing, fails, times out, or reconnects after token refresh, the client must recover missed notifications through the existing REST notification list API. This phase does not implement server-side replay from `Last-Event-ID`, Outbox, Redis Pub/Sub, or a message broker.

## 2. Backend SSE Lifecycle

- Endpoint: `GET /api/notifications/stream`
- Authentication: `Authorization: Bearer` access token
- Access token in query parameters: prohibited
- Native `EventSource` alone: not compatible with the current authentication contract because it cannot attach `Authorization`
- Required frontend shape: fetch-based SSE client, or an equivalent client that can attach request headers

`SseEmitter` timeout is one hour. Nginx `proxy_read_timeout 3600s` matches that timeout in the prod-like reverse proxy.

The backend allows multiple concurrent SSE connections per user. Connections are managed independently by server-generated connection id, so one tab disconnecting does not remove another tab's emitter.

## 3. Heartbeat

The backend sends an SSE comment heartbeat every 30 seconds through `NotificationSseService.sendHeartbeat()`.

Policy:

- Heartbeat is connection liveness traffic, not a business notification.
- Heartbeat success is not logged at INFO.
- Heartbeat success is logged only at TRACE when there is at least one successful send.
- Heartbeat send failure removes only the failed connection.

## 4. Cleanup

The service removes the target connection on:

- `onCompletion`
- `onTimeout`
- `onError`
- connected event send failure
- notification event send failure
- heartbeat send failure

Cleanup is idempotent. A user key is removed when the last connection for that user disappears.

## 5. Notification Delivery And DB Fallback

Notification creation still stores a `Notification` row first. The application publishes `NotificationCreatedEvent` in the same transaction, and the SSE listener handles it with:

- `@TransactionalEventListener(phase = AFTER_COMMIT)`
- default `fallbackExecution=false`
- `REQUIRES_NEW` read-only lookup after commit

This means SSE send happens only after the DB transaction commits. If the transaction rolls back, SSE is not sent. If SSE send fails after commit, the stored notification is not rolled back and can be recovered by the REST notification list API.

## 6. Reconnect Policy

Frontend reconnect behavior remains a client responsibility:

- On SSE `401` or `403`, call `/api/auth/reissue`.
- If reissue succeeds, reconnect SSE with the new access token in the `Authorization` header.
- If reissue fails, redirect to login.
- After reconnect, refresh the notification list through REST to recover missed events.

## 7. Explicitly Not Implemented In Phase 8-7

- Server-side `Last-Event-ID` replay
- Outbox pattern
- Redis Pub/Sub fan-out
- Message broker fan-out
- Multi-backend-instance SSE delivery guarantees
- Access token query parameter support
- HttpOnly Cookie Refresh Token migration

The current guarantee is for a single backend instance. Multi-instance deployment needs Redis Pub/Sub, a broker, or another fan-out design in a later phase.

## 8. Logging Policy

- SSE connect: INFO
- Timeout: INFO
- Completion/error cleanup: DEBUG
- Notification send failure: WARN
- Heartbeat send failure: WARN
- Heartbeat success: no INFO, TRACE only
- Unauthorized SSE attempt: Spring Security entry point/filter logging

Logs must not include access tokens, refresh tokens, credentials, DB passwords, Redis passwords, JWT secrets, AWS credentials, or admin passwords.

## 9. Verification

Code-level verification:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
git diff --check
```

Prod-like manual verification through Nginx:

```powershell
curl.exe -i -N http://localhost:8081/api/notifications/stream
```

Expected unauthenticated result: `401`.

Authenticated SSE verification requires a local test account and an access token, but token values must not be printed in reports or logs.
