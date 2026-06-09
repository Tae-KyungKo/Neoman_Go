# Phase 8 Known Risks

## 1. 결론

Phase 8의 주요 위험은 기능 구현보다 운영 환경 전환에서 발생한다.

현재 추적하는 주요 위험:

- prod/prodlike DB schema와 Entity 불일치
- CORS/SSE 운영 도메인 검증 누락
- secret 또는 credential의 Git 포함
- devtools/p6spy 같은 개발 도구의 운영 artifact 포함
- 메모리 기반 SSE emitter의 다중 인스턴스 한계
- production에서 `/dev` 검증 UI 노출
- EC2 Docker Redis + AOF 운영 책임
- Redis 외부 노출 또는 password 누락

## 2. P0 배포 차단 위험

### DB schema 자동 변경

prod/prodlike에서 `ddl-auto=create/update`를 사용하면 운영 DB schema와 데이터가 손상될 수 있다.

대응:

- prod/prodlike는 `ddl-auto=validate`만 사용한다.
- DB schema 변경은 Flyway migration으로만 반영한다.
- 운영 migration 전 snapshot/backup 기준을 확인한다.

### secret 노출

운영 DB password, Redis password, JWT secret, AWS credential, ADMIN password가 Git 또는 문서에 포함되면 즉시 rotation이 필요하다.

대응:

- secret 파일은 읽거나 출력하지 않는다.
- 문서와 `.env.example`에는 placeholder만 기록한다.
- prod/prodlike는 secret yml이 아니라 환경변수 기반으로 값을 주입한다.

### Redis 외부 노출

Redis가 `0.0.0.0:6379`로 인터넷에 공개되면 password 유무와 관계없이 심각한 보안 위험이다.

대응:

- Redis는 Docker network 또는 localhost 바인딩만 허용한다.
- Security Group에서 Redis port를 인터넷 전체에 공개하지 않는다.
- Redis password는 보조 방어선으로만 본다.

## 3. P1 운영 안정성 위험

### CORS/SSE 운영 도메인 검증 누락

운영 도메인 기준 CORS/SSE 검증이 누락되면 frontend에서 backend API 또는 SSE 연결이 실패할 수 있다.

대응:

- canonical frontend는 `https://neomango.kr`이다.
- www는 root domain으로 redirect한다.
- backend API는 `https://api.neomango.kr`이다.
- SSE 운영 URL은 `https://api.neomango.kr/api/notifications/stream`이다.
- Nginx/SSE timeout, proxy buffering, CORS는 후속 Phase에서 실제 검증한다.

### 메모리 기반 SSE emitter

현재 SSE emitter가 애플리케이션 메모리 기반이면 다중 인스턴스에서 event fan-out이 깨질 수 있다.

대응:

- Phase 8 1차 배포는 단일 Backend 인스턴스 기준으로 제한한다.
- 다중 인스턴스 전환 시 Redis Pub/Sub 또는 메시지 브로커를 재검토한다.
- MSA는 Phase 8 범위에서 제외한다.

### `/dev` 검증 UI 노출

production build에서 `/dev` 검증 UI가 노출되면 보안/신뢰성 위험이다.

대응:

- production build에서 `/dev` route가 존재하지 않는지 검증한다.
- 코드가 남아 있더라도 production routing/build에서 노출되면 안 된다.

## 4. P2 Redis 운영 위험

Phase 8 1차 배포는 비용을 고려해 EC2 Docker Redis + AOF 방식을 선택한다.

운영 권장안:

- ElastiCache Redis

1차 비용 절감안:

- EC2 Docker Redis + AOF

남은 위험:

- EC2 Docker Redis는 ElastiCache보다 backup, restore, failover 책임이 크다.
- `appendfsync everysec`는 장애 시 최대 약 1초 수준의 데이터 유실 가능성이 있다.
- Docker named volume backup/restore 절차가 없으면 EC2 disk 장애 시 Refresh Token 복구가 어렵다.
- prod/prodlike에서 `REDIS_PASSWORD`가 비어 있으면 운영 위험이다.
- `maxmemory-policy`가 `allkeys-lru` 등 eviction 정책으로 잘못 설정되면 Refresh Token key가 임의 삭제되어 예고 없는 로그아웃이 발생할 수 있다.
- Redis 장애 시 로그인, refresh token 재발급, 로그아웃이 실패할 수 있다.
- 기존 Access Token은 만료 전까지 사용 가능하므로 Redis 장애 복구 후 일부 사용자는 재로그인이 필요할 수 있다.

대응:

- AOF를 활성화하고 `appendfsync everysec`를 사용한다.
- Docker named volume을 사용한다.
- `maxmemory-policy noeviction`을 사용한다.
- Redis를 외부 인터넷에 노출하지 않는다.
- Phase 8-5 Docker Compose에서 password, AOF, named volume, restart policy, `noeviction`, network binding을 반영한다.
- backup/restore 절차는 Phase 8-5 또는 Phase 8-11에서 리허설한다.
- 운영 부담이 커지면 ElastiCache Redis 전환을 우선 검토한다.

## 5. 이번 Step에서 하지 않는 작업

Phase 8-4에서는 다음 작업을 하지 않는다.

- Dockerfile 작성 또는 수정
- Docker Compose 작성 또는 수정
- Nginx 설정 작성 또는 수정
- GitHub Actions workflow 작성 또는 수정
- Redis Pub/Sub 구현
- Redis cache, distributed lock, session store 구현
- SseEmitter 구조 변경
- ADMIN bootstrap 구현
- 운영 secret 생성, 조회, 출력
