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

이번 Phase 8-1은 위험을 문서화만 한다. 코드/설정 변경은 후속 Phase에서 수행한다.

## 2. P0 배포 차단 위험

### `ddl-auto=create`

현재 `application.yml`의 `spring.jpa.hibernate.ddl-auto=create`는 운영 위험이다.

위험:

- 운영 DB schema가 재생성될 수 있음
- 기존 데이터 손실 가능
- RDS MySQL 운영 전환 시 치명적인 장애로 이어질 수 있음

대응:

- Phase 8-2에서 profile 분리
- prod/prodlike에서 `ddl-auto=create/update` 금지
- Phase 8-3에서 Flyway baseline 기준으로 schema 관리

이번 Step에서 하지 않는 작업:

- `application.yml` 수정
- `ddl-auto` 값 변경
- Flyway migration 작성

### CORS localhost 하드코딩

현재 `SecurityConfig`의 CORS origin이 `http://localhost:5173` 기준으로 하드코딩되어 있다.

위험:

- `https://neomango.kr`에서 backend API 호출 실패
- `https://www.neomango.kr` redirect 전/후 요청 정책 불일치
- SSE stream 연결 실패

대응:

- Phase 8-2에서 CORS allowed origins를 profile/env property로 전환
- prod에서는 `https://neomango.kr`, `https://www.neomango.kr`만 허용
- local origin은 local profile에서만 허용

이번 Step에서 하지 않는 작업:

- `SecurityConfig` 수정
- CORS property 전환

### secret profile 기본 포함

현재 `spring.profiles.include: secret` 기본 포함은 운영 위험이다.

위험:

- 운영 실행이 특정 로컬 secret 파일 구조에 의존할 수 있음
- secret 주입 경로가 명확하지 않으면 재현성과 보안이 약해짐
- secret 파일을 실수로 배포하거나 커밋할 위험이 증가

대응:

- Phase 8-2에서 profile/env/secret 주입 전략 분리
- 운영 secret은 Git에 포함하지 않음
- 문서와 예시에는 placeholder만 사용

이번 Step에서 하지 않는 작업:

- secret 파일 조회
- secret 파일 출력
- secret 파일 생성

## 3. P1 운영 안정성 위험

### devtools/p6spy 운영 포함 가능성

devtools/p6spy가 `implementation` scope라면 운영 artifact에 포함될 위험이 있다.

위험:

- 운영 성능 저하
- SQL parameter 또는 민감 정보 로그 노출
- 운영 build에 개발 도구 포함

대응:

- Phase 8-2에서 scope/profile 전략 정리
- 운영 profile에서 SQL parameter 로그 제한
- prod build 결과물에 포함되는 dependency 확인

이번 Step에서 하지 않는 작업:

- `build.gradle` 수정
- dependency scope 변경

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

