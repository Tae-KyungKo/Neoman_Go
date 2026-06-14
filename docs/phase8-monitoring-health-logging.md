# Phase 8-10 Monitoring, Health, Logging

## Scope

Phase 8-10 adds the first backend health and logging baseline.

Included:

- Spring Boot Actuator dependency
- `/actuator/health`
- `/actuator/info`
- DB and Redis health through Actuator health indicators
- prod-like backend Docker healthcheck
- production-oriented log level policy

Excluded:

- Prometheus, Grafana, ELK, CloudWatch Agent, APM, and log collector setup
- automatic deployment or CD
- GitHub Actions workflow changes
- Flyway schema changes
- ADMIN bootstrap behavior changes
- SSE, Nginx, and CORS policy changes except health/info reachability through the existing proxy

## Actuator Exposure

Only these endpoints are exposed over HTTP:

- `/actuator/health`
- `/actuator/info`

The following endpoints must not be exposed in prod/prodlike:

- `/actuator/env`
- `/actuator/beans`
- `/actuator/configprops`
- `/actuator/loggers`
- `/actuator/metrics`
- `/actuator/heapdump`
- `/actuator/threaddump`

## Health Detail Policy

prod/prodlike:

```yaml
management.endpoint.health.show-details: never
```

The public health response should expose only overall status, for example:

```json
{"status":"UP"}
```

local may show details for development convenience. prod/prodlike must not.

## DB And Redis Health

Actuator includes DB and Redis health indicators because the backend already uses Spring Data JPA and Spring Data Redis.

Policy:

- DB DOWN means overall health is DOWN.
- Redis DOWN means overall health is DOWN.
- Redis is part of service availability because it stores refresh tokens.

## Security

Spring Security permits unauthenticated access only to:

- `GET /actuator/health`
- `GET /actuator/info`

Other API and SSE authentication policy remains unchanged. Access tokens must not be sent in query parameters.

## Logging

Production/prod-like log policy:

- root: `INFO`
- `com.neomango`: `INFO`
- Spring Security: `WARN`
- Hibernate SQL logging: not DEBUG in prod/prodlike
- p6spy remains a development-only dependency and is not part of the production runtime artifact

Do not log:

- password values
- JWT secrets
- DB passwords
- Redis passwords
- AWS credentials
- Authorization headers
- access tokens or refresh tokens
- full request bodies containing sensitive data

SSE policy:

- heartbeat success must not be logged at INFO
- SSE send failures remain WARN

ADMIN bootstrap policy:

- enabled/skip/success/failure state may be logged
- ADMIN password must never be logged

Application logs are written to stdout. Container file logging is not added in this phase.

## Docker Healthcheck

`docker-compose.prodlike.yml` checks backend readiness with:

```text
http://localhost:8080/actuator/health
```

The backend runtime image includes `curl` only to support the container healthcheck. The image still does not include production credentials.

## Log Inspection

First deployment log inspection uses:

```powershell
docker logs <backend-container>
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike logs backend
docker compose -f docker-compose.prodlike.yml --env-file .env.prodlike logs nginx
```

Do not print secret values while collecting or sharing logs.

## Follow-up

Phase 8-11 should cover release rehearsal and operational runbook hardening. Prometheus/Grafana/APM/log collection can be considered after this baseline is stable.
