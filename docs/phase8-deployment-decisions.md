# Phase 8 Deployment Decisions

## 1. 결론

Phase 8 배포 기준은 운영 안정성을 우선한다. 비용 때문에 일부 AWS managed service를 줄일 수는 있지만, DB schema와 secret, 운영 도메인, CORS, SSE 정책은 타협하지 않는다.

## 2. 확정 결정사항

| 항목 | 결정 |
| --- | --- |
| 서비스 도메인 | `neomango.kr` |
| 프론트 기본 URL | `https://neomango.kr` |
| 프론트 www URL | `https://www.neomango.kr` |
| 백엔드 API URL | `https://api.neomango.kr` |
| SSE URL | `https://api.neomango.kr/api/notifications/stream` |
| Frontend 목표 | S3 + CloudFront |
| Backend 목표 | EC2 Docker + Nginx |
| DB 목표 | RDS MySQL |
| Redis 권장 | ElastiCache |
| Redis 비용 대안 | EC2 Docker Redis + AOF |
| staging | 상시 운영하지 않음 |
| staging 대체 | 로컬 Docker prod-like 검증 |
| CI/CD 1차 목표 | test/lint/build 검증 자동화 |
| 자동 배포 | Phase 8 1차 범위 제외 |

## 3. 금지사항

prod/prodlike에서 금지:

- `spring.jpa.hibernate.ddl-auto=create`
- `spring.jpa.hibernate.ddl-auto=update`
- 운영 secret을 Git에 커밋
- 운영 비밀번호/JWT secret/DB password를 문서에 실제 값으로 기록
- `/dev` 검증 UI를 production build에 노출
- CORS `*` 허용과 credentials 동시 사용
- 운영 로그에 Access Token, Refresh Token, DB password, 개인정보 출력

## 4. DB schema 결정

운영 DB schema는 Flyway baseline으로 관리한다.

단순한 방법:

- JPA `ddl-auto=create/update`로 운영 schema를 자동 반영한다.

실무적인 방법:

- 현재 entity 기준으로 baseline migration을 작성한다.
- 운영 DB에는 migration으로만 schema 변경을 반영한다.
- JPA ddl-auto는 prod/prodlike에서 `validate` 또는 `none` 계열로 제한한다.

추천안:

- Phase 8-2에서 Flyway dependency와 baseline SQL을 추가한다.
- 기존 개발 DB와 운영 신규 DB의 기준점을 분리한다.
- 운영 배포 전 entity와 baseline schema의 차이를 검증한다.

## 5. Redis 결정

권장안은 ElastiCache Redis다.

비용상 EC2 Docker Redis를 사용할 경우 다음 조건을 문서화해야 한다.

- AOF 활성화
- Redis 데이터 볼륨 영속화
- EC2 재시작 시 Redis 자동 복구
- backup/restore 절차
- Refresh Token 유실 시 사용자 재로그인 정책

Refresh Token 저장소이므로 Redis 장애는 인증 UX에 직접 영향을 준다.

## 6. SSE 결정

SSE는 Phase 8에서도 유지한다.

단일 인스턴스 MVP:

- 현재 메모리 기반 emitter 저장소를 유지할 수 있다.
- EC2 1대 기준으로는 구조가 단순하다.

다중 인스턴스 확장:

- 현재 구조만으로는 다른 인스턴스에 연결된 사용자에게 실시간 이벤트를 전달할 수 없다.
- Redis Pub/Sub 또는 메시지 브로커를 도입해야 한다.

프론트 인증 방식:

- 기본 `EventSource`는 Authorization header 제어가 어렵다.
- `@microsoft/fetch-event-source` 등 header 전달 가능한 방식을 우선 검토한다.
- query string token 방식은 로그/브라우저 히스토리/프록시 노출 위험이 있으므로 기본안으로 두지 않는다.

## 7. CORS 결정

운영 허용 origin:

```text
https://neomango.kr
https://www.neomango.kr
```

로컬 허용 origin:

```text
http://localhost:5173
```

결정:

- 운영 origin과 로컬 origin을 같은 profile에 섞지 않는다.
- CORS 설정은 환경별 property로 분리한다.
- 현재 코드의 `http://localhost:5173` 하드코딩은 Phase 8-1에서 수정 대상이다.

## 8. CI/CD 결정

1차 CI/CD는 배포 자동화가 아니라 검증 자동화다.

Backend:

```text
./gradlew.bat test
```

Frontend:

```text
npm run lint
npm run build
```

현재 저장소에서는 frontend 디렉터리가 확인되지 않았으므로, frontend workflow는 실제 프론트 저장소/디렉터리 위치가 확정된 뒤 작성한다.

