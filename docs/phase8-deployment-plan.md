# Phase 8 Deployment Plan

## 1. 결론

Phase 8의 목표는 `neomango.kr` 기준 운영 배포가 가능한 구조를 만드는 것이다.

핵심 기준:

- `main`은 운영 가능한 source of truth이다.
- production deploy는 `main` 최신 커밋 직배포가 아니라 immutable version tag 기준으로 수행한다.
- 1차 production deploy 흐름은 `release/v1.0 -> main 병합 -> v1.0.0 tag 생성 -> production deploy`이다.
- staging은 상시 운영하지 않고 release branch의 prod-like 리허설로 대체한다.

## 2. 확정 도메인

| 구분 | URL |
| --- | --- |
| 서비스 도메인 | `neomango.kr` |
| canonical frontend URL | `https://neomango.kr` |
| www frontend URL | `https://www.neomango.kr` -> `https://neomango.kr` redirect |
| Backend API URL | `https://api.neomango.kr` |
| SSE URL | `https://api.neomango.kr/api/notifications/stream` |

## 3. 목표 아키텍처

### Frontend

- S3 + CloudFront
- `https://neomango.kr`를 canonical domain으로 사용
- `https://www.neomango.kr`은 canonical domain으로 redirect
- production build에서 `/dev` 검증 UI 노출 금지

### Backend

- EC2 + Docker + Nginx
- API domain은 `https://api.neomango.kr`
- SSE 운영 URL은 `https://api.neomango.kr/api/notifications/stream`
- 1차 배포는 단일 Backend 인스턴스 기준
- 다중 인스턴스 전환 시 SSE event fan-out 구조 재검토

### DB

- RDS MySQL
- Flyway baseline 기준 schema 초기화와 변경 이력 관리
- prod/prodlike에서 `ddl-auto=create/update` 금지

### Redis

- 운영 권장안: ElastiCache Redis
- 1차 비용 절감안: EC2 Docker Redis + AOF
- Phase 8 1차 배포는 EC2 Docker Redis + AOF 기준
- 1차 역할은 Refresh Token 저장소로 제한
- SSE Pub/Sub, cache, distributed lock은 Phase 8 범위 제외
- 실운영 안정화 단계에서 ElastiCache 전환 검토

## 4. 환경 전략

### local

- 개발자 로컬 실행 환경
- local 전용 profile과 local secret 사용 가능
- Redis password 없음 허용
- 운영 secret은 사용하지 않음

### prodlike

- 상시 staging 서버 대신 로컬 Docker prod-like 검증으로 대체
- prod와 동일한 profile/env/secret 주입 방식을 검증
- 실제 운영 secret은 사용하지 않음
- release branch에서 배포 리허설 기준으로 사용
- Redis `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`는 env 기반 필수 주입

### prod

- 운영 환경
- immutable tag 기준으로 배포
- 운영 secret은 Git에 포함하지 않음
- `ddl-auto=create/update` 금지
- `/dev` 검증 UI 노출 금지
- Redis `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`는 env 기반 필수 주입

## 5. CI/CD 1차 범위

Phase 8의 GitHub Actions는 자동 배포가 아니라 검증 자동화가 목표다.

포함:

- backend test
- frontend lint
- frontend build
- 가능하면 Docker image build 검증

제외:

- EC2 자동 배포
- 운영 DB migration 자동 실행
- 운영 서버 재시작 자동화

## 6. Phase 8 작업 순서

1. Phase 8-0: Phase 7.5 종료 상태와 Phase 8 배포 기준 문서화
2. Phase 8-1: 배포 방식 및 브랜치/릴리즈 전략 확정
3. Phase 8-2: profile/env/secret 분리
4. Phase 8-3: Flyway baseline 설계 및 적용
5. Phase 8-3.5: Docker MySQL 8 prodlike runtime 검증
6. Phase 8-4: Redis 운영 설정 분리와 운영 정책 문서화
7. Phase 8-5: Docker Compose prod-like 실행 환경 구성
8. Phase 8-6: Nginx reverse proxy 및 SSE 설정 검증
9. Phase 8-7: backend test workflow 구성
10. Phase 8-8: frontend lint/build workflow 구성
11. Phase 8-9: AWS 인프라 배포 준비
12. Phase 8-10: 운영 도메인 DNS/HTTPS 연결
13. Phase 8-11: 배포 리허설, rollback, 운영 인수 문서 정리

## 7. Phase 8-4 완료 기준

Phase 8-4에서는 Redis 운영 설정과 정책만 정리한다. Docker Compose 작성은 Phase 8-5 범위다.

완료 기준:

- local Redis는 `localhost:6379`와 empty password를 허용한다.
- prodlike/prod Redis는 `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` 환경변수 기반으로 실행한다.
- prodlike/prod에서 Redis password가 비어 있는 구성을 운영 위험으로 본다.
- `.env.example`과 `docs/env.example.md`에 Redis placeholder를 정리한다.
- Redis AOF, `appendfsync everysec`, named volume, `noeviction`, 외부 노출 금지 정책을 문서화한다.
- Redis 장애 시 로그인, refresh token 재발급, 로그아웃 실패 가능성을 문서화한다.
- 기존 Access Token은 Redis 장애 중에도 만료 전까지 사용 가능하다는 운영 영향을 문서화한다.

Phase 8-4에서 하지 않는 작업:

- Dockerfile 작성 또는 수정
- Docker Compose 작성 또는 수정
- Nginx 설정 작성 또는 수정
- GitHub Actions workflow 작성 또는 수정
- Redis Pub/Sub 구현
- Redis cache, distributed lock, session store 구현
- SseEmitter 구조 변경
- 운영 Redis password 생성 또는 기록

## 8. Phase 8-5에서 이어갈 작업

- Docker Compose에 Backend, MySQL prod-like, Redis 구성을 작성한다.
- Redis container를 외부 인터넷에 노출하지 않는 network/bind 구성을 반영한다.
- Redis password를 compose env placeholder로만 주입한다.
- Redis AOF, named volume, restart policy, `noeviction` 설정을 반영한다.
- Redis volume backup/restore 리허설 항목을 문서화한다.
- prod-like compose 실행 후 `compileJava`, `test`, application boot 검증을 다시 수행한다.
## Phase 8-5 Prod-like Docker 구성

Phase 8-5에서는 로컬 prod-like 검증을 위해 `Dockerfile`과 `docker-compose.prodlike.yml`을 추가한다.

- compose 대상은 backend, mysql, redis 3개 서비스로 제한한다.
- backend는 검증 편의를 위해 `8080:8080`으로 노출한다.
- mysql은 로컬 충돌 방지를 위해 `3307:3306`으로 노출한다.
- redis는 host port를 노출하지 않고 compose 내부 네트워크에서만 접근한다.
- prodlike profile은 Flyway enabled, Hibernate `ddl-auto=validate` 기준을 유지한다.
- Nginx, GitHub Actions, ADMIN bootstrap은 이번 단계 범위가 아니다.

세부 실행 방법과 cleanup 기준은 [phase8-prodlike-docker.md](./phase8-prodlike-docker.md)를 따른다.
