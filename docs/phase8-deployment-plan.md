# Phase 8 Deployment Plan

## 1. 결론

Phase 8의 목표는 `neomango.kr` 기준 운영 배포가 가능한 구조를 만드는 것이다.

Phase 8-1에서 확정한 가장 중요한 기준은 다음과 같다.

- `main`은 운영 가능한 source of truth다.
- production deploy는 `main` 최신 커밋 직배포가 아니라 immutable version tag 기준으로 수행한다.
- 1차 production deploy 흐름은 `release/v1.0 -> main 병합 -> v1.0.0 tag 생성 -> production deploy`다.
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
- `https://www.neomango.kr`는 canonical domain으로 redirect
- production build에서 `/dev` 검증 UI 노출 금지

### Backend

- EC2 + Docker + Nginx
- API domain은 `https://api.neomango.kr`
- SSE 운영 URL은 `https://api.neomango.kr/api/notifications/stream`
- 1차 배포는 단일 backend 인스턴스 기준
- 다중 인스턴스 전환 시 SSE event fan-out 구조 재검토

### DB

- RDS MySQL
- Flyway baseline 기준으로 schema 초기화와 변경 이력 관리
- prod/prodlike에서 `ddl-auto=create/update` 금지

### Redis

- 운영 권장안: ElastiCache Redis
- 1차 비용 절감안: EC2 Docker Redis + AOF
- Phase 8 1차 배포는 EC2 Docker Redis + AOF 기준
- Refresh Token 저장과 알림/SSE 보조 인프라로 사용
- 실운영 안정화 단계에서 ElastiCache 전환 검토

## 4. 환경 전략

### local

- 개발자 로컬 실행 환경
- local 전용 profile과 local secret 사용 가능
- 운영 secret은 사용하지 않음

### prodlike

- 상시 staging 서버 대신 로컬 Docker prod-like 검증으로 대체
- prod와 동일한 profile/env/secret 주입 방식을 검증
- 실제 운영 secret은 사용하지 않음
- release branch에서 배포 리허설 기준으로 활용

### prod

- 운영 환경
- immutable tag 기준으로 배포
- 운영 secret은 Git에 포함하지 않음
- `ddl-auto=create/update` 금지
- `/dev` 검증 UI 노출 금지

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
3. Phase 8-2: 운영/prodlike 설정 분리
4. Phase 8-3: Flyway baseline 설계 및 적용
5. Phase 8-4: Docker prod-like 실행 환경 구성
6. Phase 8-5: Nginx reverse proxy 및 SSE 설정 검증
7. Phase 8-6: backend test workflow 구성
8. Phase 8-7: frontend lint/build workflow 구성
9. Phase 8-8: AWS 인프라 배포 준비
10. Phase 8-9: 운영 도메인 DNS/HTTPS 연결
11. Phase 8-10: 배포 리허설 및 rollback 문서화
12. Phase 8-11: 운영 인수 문서 정리

## 7. Phase 8-2 작업 목록

Phase 8-2에서는 문서 확정 이후 실제 설정 분리를 시작한다.

해야 할 작업:

- `application-local.yml`, `application-prodlike.yml`, `application-prod.yml` 분리 설계 및 생성
- prod/prodlike에서 `ddl-auto=create/update` 제거
- CORS allowed origins를 property 기반으로 전환
- 운영 도메인 placeholder 기반 환경변수 예시 정리
- devtools/p6spy 운영 제외 전략 반영
- secret include 전략 재검토

하지 않을 작업:

- Flyway migration 작성
- Dockerfile 작성
- Nginx 설정 작성
- GitHub Actions workflow 작성
- 운영 배포 수행

## 8. 이번 Step에서 하지 않는 작업

Phase 8-1에서는 다음 작업을 하지 않는다.

- 운영 코드 수정
- `application.yml` 수정
- `SecurityConfig` 수정
- Dockerfile 작성
- Docker Compose 작성
- GitHub Actions workflow 작성
- profile 파일 생성
- Flyway migration 파일 작성
- `ddl-auto` 수정
- CORS property 전환
- Nginx 설정 작성
- secret 파일 조회 또는 출력

## 9. Phase 8-2 완료 기준

Phase 8-2에서는 profile/env/secret 분리와 CORS property 전환까지만 수행한다.

완료 기준:

- `spring.profiles.include: secret` 기본 포함 제거
- local profile에서만 `application-secret.yml` 선택 import
- prod/prodlike env 기반 DB/Redis/JWT 설정
- prod/prodlike `ddl-auto=validate`
- CORS origin property 기반 전환
- devtools/p6spy 운영 artifact 제외
- `.env.example`, `docs/env.example.md` placeholder 문서 추가

Phase 8-2에서 하지 않는 작업:

- Flyway baseline 작성
- Dockerfile 작성
- Docker Compose 작성
- Nginx 설정 작성
- GitHub Actions workflow 작성
- 실제 운영 도메인 CORS/SSE 검증

## 10. Phase 8-3 완료 기준

Phase 8-3에서는 Flyway baseline을 도입한다.

완료 기준:

- Flyway dependency 추가
- `src/main/resources/db/migration/V1__baseline_schema.sql` 작성
- prodlike/prod Flyway 활성화
- local/test Flyway 비활성화
- prodlike/prod `ddl-auto=validate` 유지
- V1 migration은 seed data 없이 DDL만 포함
- rollback은 down migration이 아니라 forward fix 또는 DB snapshot/backup 복구 기준으로 문서화

Phase 8-3에서 하지 않는 작업:

- ADMIN bootstrap 구현
- Dockerfile 작성
- Docker Compose 작성
- Nginx 설정 작성
- GitHub Actions workflow 작성
- Redis AOF/보안 설정 구현
- CORS/SSE 운영 도메인 검증
