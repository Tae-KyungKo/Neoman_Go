# Phase 8 Known Risks

## 1. 결론

Phase 8의 핵심 리스크는 기능 구현 부족보다 운영 환경 전환에서 발생한다. 특히 DB schema 자동 생성, CORS 하드코딩, secret 관리, SSE proxy 설정, Redis 운영 방식이 배포 차단 요인이다.

## 2. P0 배포 차단 리스크

### `ddl-auto=create`

현재 `src/main/resources/application.yml`에 `spring.jpa.hibernate.ddl-auto: create`가 설정되어 있다.

운영 또는 prodlike에서 이 설정이 적용되면 DB schema와 데이터가 손상될 수 있다.

대응:

- Phase 8-1에서 profile별 설정을 분리한다.
- prod/prodlike에서는 `create/update`를 금지한다.
- Phase 8-2에서 Flyway baseline을 도입한다.

### Secret profile 의존

현재 `spring.profiles.include: secret`가 기본 application 설정에 포함되어 있다.

운영 secret 파일을 직접 배포하거나 로컬 secret 구조에 의존하면 환경 재현성과 보안이 약해진다.

대응:

- 운영 secret은 파일 커밋이 아니라 환경변수, EC2 secret 파일 주입, AWS Secrets Manager 중 하나로 관리한다.
- 문서와 예시에는 placeholder만 사용한다.

### CORS localhost 하드코딩

현재 `SecurityConfig`의 CORS origin은 `http://localhost:5173`만 허용한다.

운영 프론트 `https://neomango.kr`, `https://www.neomango.kr`에서 API 호출과 SSE 연결이 실패할 수 있다.

대응:

- CORS origin을 환경별 property로 분리한다.
- prod에서는 `https://neomango.kr`, `https://www.neomango.kr`만 허용한다.

## 3. P1 운영 안정성 리스크

### SSE reverse proxy 설정

SSE는 일반 REST API보다 proxy 설정에 민감하다.

위험:

- Nginx buffering으로 이벤트가 즉시 전달되지 않을 수 있음
- proxy idle timeout이 `SseEmitter` timeout보다 짧으면 연결이 예기치 않게 끊김
- HTTPS/CORS/Auth 조합에서 preflight 또는 stream 연결 실패 가능

대응:

- Nginx location별 SSE 설정을 분리한다.
- `proxy_buffering off`를 검증한다.
- 실제 운영 도메인으로 장시간 연결 테스트를 수행한다.

### 메모리 기반 SSE emitter 저장소

현재 SSE connection은 애플리케이션 메모리의 `ConcurrentHashMap`에 저장된다.

위험:

- EC2 인스턴스가 2대 이상이면 다른 인스턴스에 연결된 사용자에게 이벤트가 전달되지 않음
- 인스턴스 재시작 시 모든 SSE connection이 끊김

대응:

- 초기 운영은 단일 인스턴스 MVP로 제한할 수 있다.
- 다중 인스턴스 전환 전 Redis Pub/Sub 또는 메시지 브로커를 도입한다.

### Redis 운영 방식

ElastiCache를 쓰지 않고 EC2 Docker Redis를 쓰면 운영 책임이 커진다.

위험:

- AOF/volume 설정 누락 시 Refresh Token 유실
- Redis 장애 시 전체 사용자 재로그인 필요
- backup/restore 절차 부재

대응:

- 비용이 허용되면 ElastiCache를 사용한다.
- EC2 Docker Redis를 쓰면 AOF, volume, restart policy, backup을 문서화하고 검증한다.

## 4. P2 품질/운영 리스크

### devtools/p6spy 운영 포함

현재 `build.gradle`에서 devtools와 p6spy가 `implementation` scope에 있다.

위험:

- 운영 artifact에 개발 도구가 포함될 수 있음
- SQL 로그가 과도하게 출력되거나 민감 정보 노출 가능
- 성능 측정이 왜곡될 수 있음

대응:

- Phase 8-1 또는 8-3에서 scope/profile을 재정리한다.
- 운영 profile에서는 SQL parameter 로그를 제한한다.

### `.env.example` 부재

현재 저장소에서 `.env.example`은 확인되지 않았다.

위험:

- 신규 환경 구성 시 필요한 변수 목록이 불명확함
- 운영 도메인/CORS/SSE URL 기준이 코드와 문서 사이에서 어긋날 수 있음

대응:

- 실제 `.env`나 운영 secret은 만들지 않는다.
- Phase 8-1에서 placeholder 기반 `.env.example` 또는 `docs/env.example.md`를 추가한다.

예시 placeholder:

```text
FRONTEND_BASE_URL=https://neomango.kr
FRONTEND_WWW_URL=https://www.neomango.kr
BACKEND_API_URL=https://api.neomango.kr
SSE_URL=https://api.neomango.kr/api/notifications/stream
CORS_ALLOWED_ORIGINS=https://neomango.kr,https://www.neomango.kr
SPRING_PROFILES_ACTIVE=prod
DB_HOST=<rds-endpoint>
DB_PORT=3306
DB_NAME=<database-name>
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
JWT_SECRET=<jwt-secret>
REDIS_HOST=<redis-host>
REDIS_PORT=6379
```

### GitHub Actions 부재

현재 `.github/workflows`는 확인되지 않았다.

위험:

- PR마다 test/lint/build 결과가 자동 검증되지 않음
- 배포 직전 회귀를 수동으로만 발견할 수 있음

대응:

- Phase 8-5에서 backend test workflow를 먼저 추가한다.
- frontend 위치가 확정되면 lint/build workflow를 추가한다.

## 5. Phase 8-1 작업 목록

1. `application-local.yml`, `application-prodlike.yml`, `application-prod.yml` 분리 설계
2. prod/prodlike `ddl-auto=create/update` 제거
3. CORS allowed origins를 property 기반으로 전환
4. 운영 도메인 placeholder 기반 `.env.example` 필요 여부 최종 결정
5. devtools/p6spy 운영 제외 전략 결정
6. SSE Nginx 설정 초안 작성
7. Flyway baseline 대상 entity/schema 목록 산출

