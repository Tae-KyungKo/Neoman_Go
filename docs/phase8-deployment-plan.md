# Phase 8 Deployment Plan

## 1. 결론

Phase 8의 목표는 너만고를 `neomango.kr` 기준 운영 도메인으로 배포 가능한 구조로 전환하는 것이다.

이번 Phase 8-0에서는 배포 구현을 하지 않고, 배포 기준과 금지사항만 고정한다.

## 2. 확정 도메인

| 구분 | URL |
| --- | --- |
| 서비스 도메인 | `neomango.kr` |
| 프론트 기본 URL | `https://neomango.kr` |
| 프론트 www URL | `https://www.neomango.kr` |
| 백엔드 API URL | `https://api.neomango.kr` |
| SSE URL | `https://api.neomango.kr/api/notifications/stream` |

## 3. 목표 아키텍처

### Frontend

- 목표: S3 + CloudFront
- 도메인: `https://neomango.kr`, `https://www.neomango.kr`
- HTTPS: ACM 인증서 사용
- production build에서는 `/dev` 검증 UI를 노출하지 않는다.

### Backend

- 목표: EC2 Docker + Nginx
- API 도메인: `https://api.neomango.kr`
- Nginx는 reverse proxy 역할을 수행한다.
- SSE endpoint는 Nginx에서 buffering off, 적절한 timeout, keep-alive 설정을 검증해야 한다.

### DB

- 목표: RDS MySQL
- `prod` / `prodlike`에서는 `ddl-auto=create`와 `ddl-auto=update`를 금지한다.
- schema는 Flyway baseline 기반으로 관리한다.
- 운영 DB 접속 정보는 환경변수 또는 secret manager 계층으로 분리한다.

### Redis

- 권장: ElastiCache Redis
- 비용 절감 대안: EC2 Docker Redis + AOF
- Refresh Token 저장소이므로 운영에서는 persistence, backup, restart 정책을 명확히 정해야 한다.

## 4. 환경 전략

### local

- 개발자 로컬 실행 환경이다.
- `ddl-auto=create`는 local에서만 허용 가능하다.
- Redis/MySQL은 Docker 또는 로컬 설치를 사용할 수 있다.

### prodlike

- staging을 상시 운영하지 않는 대신 로컬 Docker prod-like 검증으로 대체한다.
- prod와 동일한 profile 정책, CORS 정책, migration 정책을 검증한다.
- 실제 운영 secret은 사용하지 않는다.

### prod

- 운영 환경이다.
- `ddl-auto=create/update` 금지
- Flyway migration 또는 baseline 적용
- 운영 도메인 CORS만 허용
- devtools/p6spy/dev UI 노출 금지

## 5. CORS 기준

운영 백엔드는 최소한 다음 origin을 허용해야 한다.

```text
https://neomango.kr
https://www.neomango.kr
```

로컬 개발 origin은 local profile에서만 허용한다.

```text
http://localhost:5173
```

권장 설정 방향:

- CORS origin은 코드 하드코딩이 아니라 profile별 property 또는 환경변수로 관리한다.
- `Authorization` header를 허용한다.
- SSE 연결에도 동일한 인증/CORS 정책이 적용되는지 검증한다.
- credentials 허용 여부는 JWT 전달 방식에 맞춰 결정한다.

## 6. SSE 배포 기준

운영 SSE URL:

```text
https://api.neomango.kr/api/notifications/stream
```

필수 검증:

- HTTPS 환경에서 연결 가능
- Authorization header 전달 방식 확정
- Access Token 만료 시 재연결/재인증 UX 확정
- Nginx `proxy_buffering off` 검증
- Nginx/ALB idle timeout과 `SseEmitter` timeout 관계 검증
- 운영 로그에 Access Token, Refresh Token, 개인정보가 출력되지 않는지 확인

현재 구현은 메모리 기반 emitter 저장소이므로, 단일 EC2 인스턴스에서는 MVP로 동작 가능하다. 다중 인스턴스 확장 시 Redis Pub/Sub 또는 메시지 브로커가 필요하다.

## 7. CI/CD 1차 범위

Phase 8의 CI/CD 1차 목표는 자동 배포가 아니다.

우선순위:

1. backend test 자동 실행
2. frontend lint 자동 실행
3. frontend build 자동 실행
4. PR 기준 검증 실패 시 merge 차단

자동 배포는 위 검증이 안정화된 뒤 별도 Phase로 분리한다.

## 8. Phase 8 작업 순서 제안

1. Phase 8-1: 운영/prodlike 설정 분리
2. Phase 8-2: Flyway baseline 설계 및 적용
3. Phase 8-3: Dockerfile 및 로컬 prod-like compose 구성
4. Phase 8-4: Nginx reverse proxy 및 SSE 설정 검증
5. Phase 8-5: GitHub Actions backend test workflow
6. Phase 8-6: frontend lint/build workflow
7. Phase 8-7: AWS 인프라 배포 준비
8. Phase 8-8: 운영 도메인 DNS/HTTPS 연결
9. Phase 8-9: 배포 리허설 및 rollback 문서화

