# Phase 8 Deployment Decisions

## 1. 결론

Phase 8의 배포 기준은 "main 최신 커밋 직배포"가 아니라 immutable version tag 배포다.

운영 가능한 source of truth는 `main`이고, 실제 production deploy 단위는 `v1.0.0` 같은 tag로 고정한다.

기본 흐름:

```text
feature branch -> dev -> release/v1.0 -> main -> v1.0.0 tag -> production deploy
```

이번 Phase 8-1은 코드/설정 변경 없이 배포 방식과 브랜치/릴리즈 전략만 문서로 확정한다.

## 2. 확정 배포 방식

| 항목 | 결정 |
| --- | --- |
| 서비스 도메인 | `neomango.kr` |
| canonical frontend domain | `https://neomango.kr` |
| www frontend domain | `https://www.neomango.kr` -> `https://neomango.kr` redirect |
| Backend API domain | `https://api.neomango.kr` |
| SSE URL | `https://api.neomango.kr/api/notifications/stream` |
| Frontend | S3 + CloudFront |
| Backend | EC2 + Docker + Nginx |
| DB | RDS MySQL |
| Redis 운영 권장안 | ElastiCache Redis |
| Redis 1차 비용 절감안 | EC2 Docker Redis + AOF |
| 1차 서버 구조 | Backend 단일 인스턴스 |
| MSA | Phase 8 범위 아님 |
| CI/CD | 검증 자동화까지만 수행, 자동 배포 제외 |

## 3. 브랜치/릴리즈 전략

Phase 8 세부 작업은 feature branch에서 수행한다.

확정 흐름:

```text
feature branch
  -> dev 통합
  -> release/v1.0 생성
  -> release 브랜치에서 prod-like 리허설
  -> main 병합
  -> v1.0.0 tag 생성
  -> tag 기준 production deploy
```

정책:

- `main`은 운영 배포 가능한 source of truth다.
- 운영 서버는 `main`의 임의 최신 커밋이 아니라 immutable tag를 기준으로 배포한다.
- `release/v1.0`에서는 기능 추가보다 안정화, prod-like 리허설, rollback 검증에 집중한다.
- `v1.0.0` tag 생성 후에는 같은 tag의 내용을 변경하지 않는다.
- 긴급 수정은 새 patch tag를 만든다. 예: `v1.0.1`

세부 정책은 [phase8-release-strategy.md](./phase8-release-strategy.md)를 기준으로 한다.

## 4. main 브랜치 정책

`main`에 포함 가능:

- 운영 실행에 안전한 source code
- local/test/prod profile 구조
- placeholder 기반 예시 설정 문서
- 테스트 코드
- 로컬 개발 편의를 위한 코드 또는 설정
- prod build에서 비활성화되는 검증 코드

`main`에 포함하면 안 되는 것:

- secret 파일
- 실제 운영 비밀번호
- 실제 JWT secret
- 실제 DB password
- 실제 Redis password
- 운영 credential이 포함된 `.env`
- production build 또는 운영 라우팅에서 노출되는 `/dev` 검증 UI

중요한 기준:

- main에서 local/test 코드를 무조건 삭제하는 전략은 쓰지 않는다.
- 대신 profile/env/secret/build 설정으로 운영에서 비활성화한다.
- `/dev` 검증 UI는 코드가 존재하더라도 production build 또는 운영 라우팅에서 노출되면 안 된다.

## 5. Frontend 결정

배포 방식:

- S3 + CloudFront
- `https://neomango.kr`를 canonical frontend domain으로 사용
- `https://www.neomango.kr`는 `https://neomango.kr`로 redirect

운영 기준:

- production build에서 `/dev` route를 노출하지 않는다.
- 현재 프론트엔드에서는 이미 `/dev` 라우팅을 삭제한 상태로 본다.
- Legacy 검증 UI가 운영 환경에서 노출되면 보안/신뢰성 위험으로 판단한다.

이번 Step에서 하지 않는 작업:

- S3 bucket 생성
- CloudFront distribution 생성
- DNS/HTTPS 연결
- frontend workflow 작성

## 6. Backend 결정

배포 방식:

- EC2 + Docker + Nginx
- Backend API domain은 `https://api.neomango.kr`로 분리
- SSE 운영 URL은 `https://api.neomango.kr/api/notifications/stream`

1차 서버 구조:

- Backend는 단일 인스턴스 기준으로 배포한다.
- 현재 메모리 기반 `SseEmitter` 구조는 단일 인스턴스 전제에서는 허용한다.
- 다중 인스턴스 전환 시 Redis Pub/Sub 또는 메시지 브로커를 재검토한다.
- MSA는 Phase 8 범위가 아니다. 추후 고도화 과정에서 실제 필요가 확인될 때 검토한다.

이번 Step에서 하지 않는 작업:

- Dockerfile 작성
- Docker Compose 작성
- Nginx 설정 작성
- application profile 파일 생성
- CORS property 전환

## 7. DB 결정

배포 방식:

- RDS MySQL 사용
- 운영 DB 스키마 초기화와 변경 이력은 Flyway 기준으로 관리
- prod/prodlike 환경에서 `ddl-auto=create`와 `ddl-auto=update` 금지

권장 운영 기준:

- 운영 DB schema 변경은 migration 파일로만 반영한다.
- 운영 profile에서는 JPA schema 자동 변경을 허용하지 않는다.
- Flyway baseline은 Phase 8-2 이후 작업에서 설계한다.

이번 Step에서 하지 않는 작업:

- Flyway dependency 추가
- migration 파일 작성
- `ddl-auto` 설정 변경
- DB schema 수정

## 8. Redis 결정

운영 권장안:

- ElastiCache Redis

1차 비용 절감안:

- EC2 Docker Redis + AOF

Phase 8 1차 배포는 비용을 고려해 EC2 Docker Redis + AOF 방식으로 간다.

사용 목적:

- Refresh Token 저장
- 알림/SSE 관련 보조 인프라

주의:

- EC2 Docker Redis + AOF는 비용상 선택한 1차 배포안이다.
- 관리형 Redis 대비 backup, restore, 장애 대응, restart policy 책임이 크다.
- 실운영 안정화 단계에서 ElastiCache Redis로 전환한다.

## 9. ADMIN bootstrap 결정

초기 ADMIN은 one-time runner 또는 command 방식으로 생성한다.

정책:

- 일반 회원가입 API에서 `role=ADMIN` 주입은 허용하지 않는다.
- DB 직접 insert는 원칙적으로 사용하지 않는다.
- bootstrap command는 재실행 안전성, 감사 가능성, secret 노출 방지를 고려해야 한다.

이번 Step에서 하지 않는 작업:

- ADMIN bootstrap 구현
- command runner 구현
- 운영 ADMIN 계정 생성

## 10. CI/CD 결정

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

## 11. 이번 Step에서 하지 않는 작업

Phase 8-1에서는 다음 작업을 하지 않는다.

- 운영 코드 수정
- `application.yml` 수정
- `SecurityConfig` 수정
- Dockerfile 작성
- Docker Compose 작성
- GitHub Actions workflow 작성
- `application-local.yml`, `application-prodlike.yml`, `application-prod.yml` 생성
- Flyway migration 파일 작성
- `ddl-auto` 수정
- CORS property 전환
- Nginx 설정 작성
- secret 파일 조회 또는 출력

