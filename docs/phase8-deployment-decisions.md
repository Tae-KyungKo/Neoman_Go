# Phase 8 Deployment Decisions

## 16. Phase 8-10 Monitoring, Health, Logging Decision

Backend health and logging are implemented as a minimal production baseline.

- Dependency: `spring-boot-starter-actuator`
- Web exposure: `health,info` only
- prod/prodlike health details: `never`
- DB and Redis indicators are part of overall health
- Public unauthenticated paths: `GET /actuator/health`, `GET /actuator/info`
- Non-exposed actuator endpoints such as env, beans, configprops, loggers, metrics, heapdump, and threaddump remain unavailable
- prod/prodlike log levels: root INFO, `com.neomango` INFO, Spring Security WARN, Hibernate SQL not DEBUG
- Runtime logs go to stdout
- prod-like backend healthcheck uses `/actuator/health`
- No Prometheus, Grafana, ELK, CloudWatch Agent, APM, CD, or GitHub Actions change is included

## 15. Phase 8-9 Backend CI Decision

Backend CI is implemented as validation only.

- Workflow file: `.github/workflows/backend-ci.yml`
- Trigger branches: `dev`, `main`, `release/**`
- Manual trigger: `workflow_dispatch`
- Runner: `ubuntu-latest`
- Java: Temurin 17
- Gradle cache: enabled
- Redis: CI service container, exposed as `localhost:6379`, no password
- Environment: `SPRING_PROFILES_ACTIVE=test`, `ADMIN_BOOTSTRAP_ENABLED=false`
- Commands: `compileJava`, `test`, Docker image build
- Docker image push: prohibited
- GitHub Secrets: not required
- AWS, ECR, EC2, SSH, SCP: excluded
- Frontend workflow: excluded from this backend repository

Branch protection is a GitHub UI follow-up, not a repository code change.

## 1. 결론

Phase 8 운영 배포는 `main` 최신 커밋 직배포가 아니라 immutable version tag 기준으로 수행한다.

기본 흐름:

```text
feature branch -> dev -> release/v1.0 -> main -> v1.0.0 tag -> production deploy
```

`main`은 운영 가능한 source of truth이고, 실제 production deploy 단위는 `v1.0.0` 같은 tag로 고정한다.

## 2. 확정 도메인

| 항목 | 결정 |
| --- | --- |
| 서비스 도메인 | `neomango.kr` |
| canonical frontend domain | `https://neomango.kr` |
| www frontend domain | `https://www.neomango.kr` -> `https://neomango.kr` redirect |
| Backend API domain | `https://api.neomango.kr` |
| SSE URL | `https://api.neomango.kr/api/notifications/stream` |

## 3. 배포 방식

| 영역 | 결정 |
| --- | --- |
| Frontend | S3 + CloudFront |
| Backend | EC2 + Docker + Nginx |
| DB | RDS MySQL |
| Redis 운영 권장안 | ElastiCache Redis |
| Redis 1차 비용 절감안 | EC2 Docker Redis + AOF |
| 1차 서버 구조 | Backend 단일 인스턴스 |
| MSA | Phase 8 범위 아님 |
| CI/CD | 자동 배포 제외, 검증 자동화 중심 |

## 4. 브랜치/릴리즈 전략

Phase 8 세부 작업은 feature branch에서 수행한다.

```text
feature branch
  -> dev 통합
  -> release/v1.0 생성
  -> release branch에서 prod-like 리허설
  -> main 병합
  -> v1.0.0 tag 생성
  -> tag 기준 production deploy
```

정책:

- `main`은 운영 배포 가능한 source of truth이다.
- 운영 서버는 `main` 임의 최신 커밋이 아니라 immutable tag 기준으로 배포한다.
- `release/v1.0`에서는 기능 추가보다 안정화, prod-like 리허설, rollback 검증에 집중한다.
- 같은 tag의 내용은 변경하지 않는다.
- 긴급 수정은 patch tag를 새로 만든다. 예: `v1.0.1`

자세한 기준은 [phase8-release-strategy.md](./phase8-release-strategy.md)를 따른다.

## 5. main 브랜치 정책

`main`에 포함 가능:

- 운영 실행에 안전한 source code
- local/test/prod profile 구조
- placeholder 기반 예시 설정 문서
- 테스트 코드
- 로컬 개발 편의를 위한 코드 또는 설정
- production build에서 비활성화되는 검증 코드

`main`에 포함 금지:

- secret 파일
- 실제 운영 비밀번호
- 실제 JWT secret
- 실제 DB password
- 실제 Redis password
- 운영 credential이 포함된 `.env`
- production build 또는 운영 routing에서 노출되는 `/dev` 검증 UI

main에서 local/test 코드를 무조건 삭제하는 전략은 사용하지 않는다. 대신 profile/env/secret/build 설정으로 운영에서 비활성화한다.

## 6. Frontend 결정

- S3 + CloudFront로 배포한다.
- `https://neomango.kr`를 canonical frontend domain으로 사용한다.
- `https://www.neomango.kr`은 `https://neomango.kr`로 redirect한다.
- production build에서는 `/dev` route를 노출하지 않는다.
- Legacy 검증 UI가 운영 환경에서 노출되면 보안/신뢰성 위험으로 본다.

이번 Step에서 하지 않는 작업:

- S3 bucket 생성
- CloudFront distribution 생성
- DNS/HTTPS 연결
- frontend workflow 작성

## 7. Backend 결정

- EC2 + Docker + Nginx로 배포한다.
- Backend API domain은 `https://api.neomango.kr`로 분리한다.
- SSE 운영 URL은 `https://api.neomango.kr/api/notifications/stream`이다.
- 1차 배포는 단일 Backend 인스턴스 기준이다.
- 현재 메모리 기반 `SseEmitter` 구조는 단일 인스턴스 전제에서만 허용한다.
- 다중 인스턴스 전환 시 Redis Pub/Sub 또는 메시지 브로커를 재검토한다.
- MSA는 Phase 8 범위가 아니다.

이번 Step에서 하지 않는 작업:

- Dockerfile 작성
- Docker Compose 작성
- Nginx 설정 작성
- SseEmitter 구조 변경

## 8. DB 결정

- RDS MySQL을 사용한다.
- 운영 DB schema 초기화와 변경 이력은 Flyway 기준으로 관리한다.
- prod/prodlike 환경에서 `ddl-auto=create/update`는 금지한다.
- prodlike/prod는 Flyway enabled + Hibernate `ddl-auto=validate` 기준이다.
- `V1__baseline_schema.sql`에는 seed data와 ADMIN 계정을 넣지 않는다.
- `baselineOnMigrate=true`는 사용하지 않는다.

자세한 기준은 [phase8-flyway-baseline.md](./phase8-flyway-baseline.md)를 따른다.

## 9. Redis 결정

Phase 8-4부터 Redis 운영 정책은 [phase8-redis-operating-policy.md](./phase8-redis-operating-policy.md)를 기준으로 한다.

1차 역할:

- Redis는 Refresh Token 저장소로 사용한다.
- SSE Pub/Sub, cache, distributed lock, session store는 Phase 8 범위에서 구현하지 않는다.
- 다중 Backend 인스턴스로 전환할 때 Redis Pub/Sub 또는 메시지 브로커를 재검토한다.

배포 방식:

- 1차 배포는 비용을 고려해 EC2 Docker Redis + AOF 방식으로 간다.
- 운영 권장안은 ElastiCache Redis이다.
- 운영 안정화, 장애 대응 요구 증가, 백업/복구 자동화 필요 시 ElastiCache로 전환한다.

보안:

- Redis는 외부 인터넷에 노출하지 않는다.
- 같은 Docker network 또는 localhost 바인딩만 허용한다.
- `0.0.0.0:6379` 공개 노출은 금지한다.
- Redis password는 보조 방어선이지 외부 공개의 근거가 아니다.

profile 정책:

- local은 Redis password 없음이 허용된다.
- prodlike/prod는 `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`를 환경변수로 필수 주입한다.
- prod/prodlike에서 `REDIS_PASSWORD`가 비어 있으면 운영 위험이다.

persistence와 memory:

- AOF를 활성화한다.
- `appendfsync everysec`를 사용한다.
- Redis data는 Docker named volume으로 영속화한다.
- `maxmemory-policy noeviction`을 사용한다.
- Refresh Token Redis TTL은 JWT refresh token 만료 시간과 동일하게 저장한다.

이번 Step에서 하지 않는 작업:

- Docker Compose 작성
- Redis Pub/Sub 구현
- SseEmitter 구조 변경
- 운영 Redis password 생성 또는 기록

## 10. ADMIN bootstrap 결정

초기 ADMIN은 one-time runner 또는 command 방식으로 생성한다.

정책:

- 일반 회원가입 API에서 `role=ADMIN` 주입은 허용하지 않는다.
- DB 직접 insert는 원칙적으로 사용하지 않는다.
- bootstrap command는 재실행 안전성, 감사 가능성, secret 노출 방지를 고려해야 한다.

이번 Step에서 하지 않는 작업:

- ADMIN bootstrap 구현
- command runner 구현
- 운영 ADMIN 계정 생성

## 11. CI/CD 결정

Phase 8의 GitHub Actions는 자동 배포를 하지 않는다.

포함 범위:

- backend test
- frontend lint/build
- 가능하면 Docker image build 검증

제외 범위:

- EC2 자동 배포
- production 서버 SSH 배포
- 운영 DB migration 자동 실행
- CloudFront invalidation 자동화

EC2 자동 배포는 Phase 8 이후 고도화 대상으로 둔다.
## 12. Phase 8-5 Prod-like Docker Decision

Phase 8-5의 Docker Compose는 실제 운영 배포 구성이 아니라 로컬 운영 유사 검증 구성이다.

- 포함 서비스: backend, mysql, redis
- 제외 서비스: frontend, nginx, certbot, cloudfront, s3, GitHub Actions
- backend host port: `8080`
- mysql host port: `3307`
- redis host port: 없음
- backend profile: `prodlike`
- database: `neomango_prodlike`
- Redis persistence: AOF enabled, `appendfsync everysec`
- Redis eviction: `noeviction`
- secret 주입: `.env.prodlike` 로컬 파일 또는 동등한 환경변수

`.env.prodlike.example`은 placeholder만 포함하며 실제 credential을 기록하지 않는다. 실제 `.env.prodlike`은 Git에 포함하지 않는다.
## 13. Phase 8-6 Nginx/CORS/SSE Decision

Phase 8-6 adds Nginx to the local prod-like compose stack for reverse proxy verification.

- prod-like Nginx host port: `8081`
- Nginx container port: `80`
- backend upstream: `http://backend:8080`
- SSE path: `/api/notifications/stream`
- production TLS/443/certbot/DNS: not applied in this phase
- production CORS origin: `https://neomango.kr`
- `https://www.neomango.kr`: redirect target, not a default allowed origin
- CORS credentials: `false`
- authentication contract: JSON token response plus `Authorization: Bearer`
- Refresh Token HttpOnly Cookie migration: not in this phase
- SSE token query parameter: prohibited
- native `EventSource` alone: not compatible with the current Authorization header policy

The generated Spring Security password warning was caused by default user auto-configuration. The application now disables form login and HTTP Basic and defines a failing `UserDetailsService` to prevent default user exposure in the JWT-only API.

## 14. Phase 8-7 SSE Reconnect/Fallback Decision

Notification DB storage is the source of truth. SSE is a realtime helper channel.

- `SseEmitter` timeout: one hour
- Heartbeat: 30-second SSE comment
- Multiple connections per user: allowed
- Cleanup: completion, timeout, error, connected send failure, notification send failure, heartbeat send failure
- Event timing: `AFTER_COMMIT`
- `fallbackExecution`: false
- Missed event recovery: REST notification list API
- Server-side `Last-Event-ID` replay: not implemented in Phase 8-7
- Outbox, Redis Pub/Sub, broker fan-out: not implemented in Phase 8-7
- Access token query parameter: prohibited
- Native `EventSource` alone: not compatible with the current Authorization header policy

See [phase8-sse-reconnect-fallback.md](./phase8-sse-reconnect-fallback.md).
## 17. Phase 8-11A Production Deployment Docs Decision

Phase 8-11A is documentation and template preparation only.

- No AWS resources are created.
- No production server connection is performed.
- No GitHub Actions workflow or CD is added.
- Production backend target remains EC2 + Docker + host Nginx.
- Production DB target remains RDS MySQL.
- First production Redis target remains EC2 Docker Redis with AOF and password.
- Production compose excludes MySQL because RDS is used.
- `.env.prod.example` contains placeholders only.
- `.env.prod` must never be committed.
- Production Nginx template keeps SSE proxy settings and permits health/info proxying.
- Rollback is tag-based for backend code; DB rollback is snapshot/forward-fix based.
