# Phase 8 Redis Operating Policy

## 1. 결론

Phase 8 1차 운영에서 Redis는 Refresh Token 저장소로만 사용한다.

SSE Pub/Sub, cache, distributed lock, session store는 Phase 8 범위에 포함하지 않는다. 다중 Backend 인스턴스로 전환할 때 Redis Pub/Sub 또는 메시지 브로커를 재검토한다.

## 2. 배포 방식

1차 배포안:

- Backend와 같은 EC2에서 Docker Redis를 실행한다.
- AOF를 활성화한다.
- Docker named volume으로 `/data`를 영속화한다.
- 비용 절감을 위한 1차 운영안이다.

운영 권장안:

- ElastiCache Redis
- 운영 안정화, 장애 대응 요구 증가, 백업/복구 자동화 필요, 단일 EC2 장애 영향 축소가 필요해지면 전환한다.

## 3. 외부 노출 금지

Redis는 외부 인터넷에 노출하지 않는다.

허용:

- 같은 Docker network 내부 접근
- EC2 localhost 바인딩

금지:

- `0.0.0.0:6379` 공개 노출
- Security Group에서 인터넷 전체에 Redis port 공개
- Redis password가 있다는 이유로 외부 공개를 허용하는 구성

Redis password는 보조 방어선이다. 네트워크 격리가 기본 방어선이다.

## 4. Password 정책

local:

- password 없음 허용
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`
- `REDIS_PASSWORD` empty 허용

prodlike/prod:

- `REDIS_HOST` 필수
- `REDIS_PORT` 필수
- `REDIS_PASSWORD` 필수
- 실제 password 값은 코드, 문서, Git history에 기록하지 않는다.

prod/prodlike에서 `REDIS_PASSWORD`가 비어 있으면 운영 위험으로 본다.

## 5. Persistence 정책

Phase 8 1차 운영에서는 AOF를 사용한다.

```text
appendonly yes
appendfsync everysec
```

이유:

- Redis가 Refresh Token 저장소이므로 재시작 시 token key 유실을 줄여야 한다.
- RDB snapshot만 사용하는 방식보다 최근 쓰기 유실 가능성을 줄인다.

한계:

- `appendfsync everysec`는 장애 시 최대 약 1초 수준의 데이터 유실 가능성이 있다.
- EC2 disk 장애, volume 손상, 잘못된 운영 명령은 별도 backup/restore 정책 없이는 복구가 어렵다.

## 6. Volume 정책

Docker named volume을 사용한다.

```text
neomango_redis_data
```

1차 정책에서 host path 직접 바인딩은 사용하지 않는다.

backup/restore 절차는 Phase 8-5 Docker Compose 작성 또는 Phase 8-11 운영 인수 문서에서 구체화한다.

## 7. Memory와 Eviction 정책

Refresh Token 저장소에서는 Redis가 임의로 token key를 제거하면 예고 없는 로그아웃이 발생한다.

따라서 `maxmemory-policy`는 다음으로 고정한다.

```text
maxmemory-policy noeviction
```

사용하지 않는 정책:

- `allkeys-lru`
- `volatile-lru`
- `allkeys-random`
- token key를 임의 eviction할 수 있는 정책

메모리 부족은 정상적인 정리 정책이 아니라 운영 장애로 본다. 모니터링, 증설, TTL 점검, 불필요 key 정리로 대응한다.

## 8. Refresh Token TTL 정책

Refresh Token 저장 시 Redis TTL은 JWT refresh token 만료 시간과 동일하게 설정한다.

현재 코드 기준:

- `RefreshTokenService`는 `jwt.refresh-token-validity-in-seconds` 값을 TTL로 사용한다.
- Redis TTL 없이 저장하지 않는다.
- Redis TTL을 JWT refresh token 만료 시간보다 길게 두지 않는다.

검증 기준:

- `RefreshTokenServiceTest`에서 저장된 refresh token key TTL이 0보다 크고 JWT refresh token validity 이하인지 검증한다.

## 9. Redis 장애 시 서비스 영향

Redis 장애 시 영향:

- 로그인 시 Refresh Token 저장 실패 가능
- refresh token 재발급 실패 가능
- 로그아웃 시 Refresh Token 삭제 실패 가능
- 기존 Access Token은 만료 전까지 사용 가능

Phase 8에서는 DB fallback을 구현하지 않는다.

운영 대응:

- Redis 복구 후 사용자는 필요 시 재로그인해야 할 수 있다.
- 장애 원인은 Redis process, Docker container, volume, EC2 disk, memory pressure, network binding 순서로 확인한다.
- 반복 장애 또는 복구 부담이 커지면 ElastiCache Redis 전환을 우선 검토한다.

## 10. 이번 Step에서 하지 않는 작업

Phase 8-4에서는 다음 작업을 하지 않는다.

- Dockerfile 작성 또는 수정
- Docker Compose 작성 또는 수정
- Nginx 설정 작성 또는 수정
- GitHub Actions workflow 작성 또는 수정
- Redis Pub/Sub 구현
- cache, distributed lock, session store 구현
- SseEmitter 구조를 Redis Pub/Sub으로 변경
- RefreshTokenService 핵심 동작 변경
- 운영 Redis password 생성 또는 기록

Docker Compose에 Redis AOF, password, named volume, network binding, restart policy를 반영하는 작업은 Phase 8-5 범위다.
## 11. Phase 8-5 Compose Reflection

`docker-compose.prodlike.yml` reflects the Phase 8 Redis policy as follows.

- Redis has no host port mapping.
- Backend accesses Redis by the compose service name `redis`.
- `REDIS_PASSWORD` is required through the prod-like env file.
- AOF is enabled with `appendfsync everysec`.
- `maxmemory-policy noeviction` is set.
- Redis data is stored in the Docker named volume `neomango_redis_prodlike_data`.

This does not introduce Redis Pub/Sub, cache, distributed lock, or session storage.
