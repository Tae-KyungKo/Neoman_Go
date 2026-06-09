# Phase 8 Known Risks

## 1. 결론

Phase 8의 주요 위험은 기능 구현보다 운영 환경 전환에서 발생한다.

현재 확인된 핵심 위험은 다음이다.

- `application.yml`의 `ddl-auto=create`
- `SecurityConfig`의 CORS localhost 하드코딩
- `spring.profiles.include: secret` 기본 포함
- devtools/p6spy 운영 artifact 포함 가능성
- 메모리 기반 SSE emitter의 다중 인스턴스 한계
- production에서 `/dev` 검증 UI 노출 위험
- EC2 Docker Redis + AOF 운영 책임

Phase 8-2에서는 profile/env/secret 분리로 일부 위험을 줄인다. 다만 Flyway baseline, Redis 운영 세부 정책, Nginx/SSE 운영 검증은 후속 Phase에서 계속 추적한다.

## 2. P0 배포 차단 위험

### `ddl-auto=create`

Phase 8-0에서 확인된 `application.yml`의 `spring.jpa.hibernate.ddl-auto=create`는 운영 위험이었다. Phase 8-2에서는 prod/prodlike를 `validate`로 분리하되, Flyway baseline이 아직 없으므로 prod/prodlike 실제 실행은 Phase 8-3 전까지 성공을 보장하지 않는다.

위험:

- 운영 DB schema가 재생성될 수 있음
- 기존 데이터 손실 가능
- RDS MySQL 운영 전환 시 치명적인 장애로 이어질 수 있음

대응:

- Phase 8-2에서 profile 분리
- prod/prodlike에서 `ddl-auto=create/update` 금지
- Phase 8-3에서 Flyway baseline 기준으로 schema 관리

이번 Step에서 하지 않는 작업:

- Flyway migration 작성

### CORS localhost 하드코딩

Phase 8-0에서 확인된 `SecurityConfig`의 CORS origin 하드코딩은 운영 위험이었다. Phase 8-2에서는 CORS origin을 property 기반으로 전환한다.

위험:

- `https://neomango.kr`에서 backend API 호출 실패
- `https://www.neomango.kr` redirect 전/후 요청 정책 불일치
- SSE stream 연결 실패

대응:

- Phase 8-2에서 CORS allowed origins를 profile/env property로 전환
- prod에서는 `https://neomango.kr`, `https://www.neomango.kr`만 허용
- local origin은 local profile에서만 허용

이번 Step에서 하지 않는 작업:

- 실제 운영 도메인 CORS 검증
- Nginx/SSE 경유 검증

### secret profile 기본 포함

Phase 8-0에서 확인된 `spring.profiles.include: secret` 기본 포함은 운영 위험이었다. Phase 8-2에서는 기본 include를 제거하고 local에서만 secret 파일을 선택적으로 import한다.

위험:

- 운영 실행이 특정 로컬 secret 파일 구조에 의존할 수 있음
- secret 주입 경로가 명확하지 않으면 재현성과 보안이 약해짐
- secret 파일을 실수로 배포하거나 커밋할 위험이 증가

대응:

- Phase 8-2에서 profile/env/secret 주입 전략 분리
- `application.yml`의 secret 기본 include 제거
- local에서만 `application-secret.yml` 선택 import
- prod/prodlike는 env 기반으로 secret 주입
- 운영 secret은 Git에 포함하지 않음
- 문서와 예시에는 placeholder만 사용

이번 Step에서 하지 않는 작업:

- secret 파일 조회
- secret 파일 출력
- secret 파일 생성

## 3. P1 운영 안정성 위험

### devtools/p6spy 운영 포함 가능성

Phase 8-0에서 devtools/p6spy가 `implementation` scope라면 운영 artifact에 포함될 위험이 있었다. Phase 8-2에서는 개발 전용 scope로 분리한다.

위험:

- 운영 성능 저하
- SQL parameter 또는 민감 정보 로그 노출
- 운영 build에 개발 도구 포함

대응:

- Phase 8-2에서 devtools/p6spy를 운영 artifact에 포함되지 않도록 scope 정리
- 운영 profile에서 SQL parameter 로그 제한
- prod build 결과물에 포함되는 dependency 확인

이번 Step에서 하지 않는 작업:

- 운영 artifact 구성 결과에 대한 Docker image 검증

### 메모리 기반 SSE emitter

현재 SSE emitter가 애플리케이션 메모리 기반이면 다중 인스턴스에서 한계가 있다.

단일 인스턴스 기준:

- Phase 8 1차 배포에서는 허용 가능
- connection이 같은 backend instance에 있으므로 실시간 전송 구조가 단순함

다중 인스턴스 기준:

- 다른 인스턴스에 연결된 사용자에게 이벤트를 전달할 수 없음
- Redis Pub/Sub 또는 메시지 브로커가 필요
- load balancer sticky session만으로 event fan-out 문제를 해결할 수 없음

대응:

- 1차 배포는 단일 backend instance로 제한
- 다중 인스턴스 전환 시 Redis Pub/Sub 또는 broker 재검토
- MSA는 Phase 8 범위에서 제외

### `/dev` 검증 UI 노출

production build에서 `/dev` 검증 UI가 노출되면 보안/신뢰성 위험이다.

위험:

- 내부 검증용 API 호출 흐름 노출
- 사용자에게 미완성 기능 또는 debug 화면 노출
- 운영 신뢰도 저하

대응:

- 현재 프론트엔드에서는 이미 `/dev` 라우팅을 삭제한 상태로 본다.
- production build에서 `/dev` route가 존재하지 않는지 검증한다.
- 코드가 남아 있더라도 production routing/build에서 노출되면 안 된다.

## 4. P2 인프라 운영 위험

### EC2 Docker Redis + AOF

Phase 8 1차 배포는 비용을 고려해 EC2 Docker Redis + AOF 방식을 선택한다.

운영 권장안:

- ElastiCache Redis

1차 비용 절감안:

- EC2 Docker Redis + AOF

위험:

- AOF/volume 설정 누락 시 Refresh Token 유실
- EC2 장애 시 Redis 복구 책임이 개발자에게 있음
- backup/restore 검증이 없으면 장애 대응이 어려움
- 알림/SSE 보조 인프라로 사용할 경우 장애 영향 범위가 커질 수 있음

대응:

- AOF 활성화
- Redis volume 영속화
- restart policy 명시
- backup/restore 절차 문서화
- 실운영 안정화 단계에서 ElastiCache Redis 전환

### tag 기준 배포 미준수

main 최신 커밋을 직접 운영 배포하면 배포 단위가 불명확해진다.

위험:

- 어떤 코드가 운영에 배포됐는지 추적하기 어려움
- rollback 기준이 불명확함
- release branch 리허설 결과와 운영 배포 코드가 달라질 수 있음

대응:

- production deploy는 `v1.0.0` 같은 immutable tag 기준으로 수행
- `release/v1.0`에서 prod-like 리허설 후 main 병합
- main 병합 후 tag 생성
- 같은 tag는 변경하지 않음

## 5. 이번 Step에서 하지 않는 작업

Phase 8-2에서는 다음 작업을 하지 않는다.

- Dockerfile 작성
- Docker Compose 작성
- GitHub Actions workflow 작성
- Flyway migration 파일 작성
- Nginx 설정 작성
- secret 파일 조회 또는 출력

## 6. Phase 8-2 이후 남는 위험

Profile/env/secret 분리를 하더라도 다음 위험은 후속 Phase에서 계속 추적한다.

- prod/prodlike의 `ddl-auto=validate`는 Flyway baseline이 없으면 실제 실행이 실패할 수 있다.
- Flyway baseline은 Phase 8-3 범위다.
- EC2 Docker Redis + AOF의 비밀번호, AOF, volume, backup/restore 세부 정책은 Phase 8-4 범위다.
- Nginx proxy buffering, SSE timeout, 실제 운영 도메인 CORS 검증은 Phase 8-6 범위다.
- `.env.example`과 `docs/env.example.md`에는 placeholder만 있으므로 운영 secret 주입 방식은 배포 전 별도 검증이 필요하다.

## 7. Phase 8-3 이후 남는 위험

Flyway baseline을 추가해도 다음 위험은 남는다.

- prodlike/prod `validate`는 실제 MySQL 8 환경에서 한 번 더 검증해야 한다.
- V1 baseline은 현재 Entity 기준이므로 이후 Entity 변경은 반드시 새 migration으로 반영해야 한다.
- `baselineOnMigrate=true`를 사용하지 않으므로 기존 데이터가 있는 DB에 바로 적용하는 전환 시나리오는 별도 설계가 필요하다.
- down migration을 작성하지 않으므로 운영 migration 전 snapshot/backup 절차가 필수다.
- Redis AOF/보안 설정, Nginx/SSE 운영 검증, ADMIN bootstrap은 각각 후속 Phase 범위다.
