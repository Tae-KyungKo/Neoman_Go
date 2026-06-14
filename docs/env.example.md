# Environment Variable Example

## 1. 목적

이 문서는 Phase 8 기준 profile/env/secret 분리 구조에서 필요한 환경변수를 정리한다.

실제 운영 secret 값은 이 문서와 `.env.example`에 기록하지 않는다. 모든 민감값은 placeholder로만 표현한다.

## 2. 도메인

```text
FRONTEND_BASE_URL=https://neomango.kr
FRONTEND_WWW_URL=https://www.neomango.kr
BACKEND_API_URL=https://api.neomango.kr
SSE_URL=https://api.neomango.kr/api/notifications/stream
CORS_ALLOWED_ORIGINS=https://neomango.kr
```

`https://www.neomango.kr`은 root domain으로 redirect한다. redirect 전 요청이나 운영 전환 중 예외가 필요하면 `CORS_ALLOWED_ORIGINS`에 명시적으로 추가한다.

## 3. Spring profile

```text
SPRING_PROFILES_ACTIVE=prod
```

지원 profile:

- `local`
- `test`
- `prodlike`
- `prod`

`application-secret.yml`은 local 전용 secret 파일로 간주한다. prod/prodlike는 secret yml이 아니라 환경변수로 값을 주입한다.

## 4. Database

```text
DB_URL=jdbc:mysql://<host>:3306/<database>?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
```

prod/prodlike에서는 `ddl-auto=create/update`를 사용하지 않는다. DB schema는 Flyway migration으로 관리하고 Hibernate는 `validate`만 수행한다.

## 5. Redis

```text
REDIS_HOST=<redis-host>
REDIS_PORT=6379
REDIS_PASSWORD=<redis-password>
```

local profile은 password 없는 Redis를 허용한다. prod/prodlike에서는 `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`를 모두 환경변수로 주입해야 하며 실제 password는 Git에 기록하지 않는다.

Phase 8 1차 운영 Redis는 비용 절감을 위해 EC2 Docker Redis + AOF 기준으로 간다. 운영 권장안은 ElastiCache Redis이며, 실운영 안정화 또는 장애 대응 요구가 커지면 전환한다.

운영 정책 placeholder:

```text
REDIS_APPENDONLY=yes
REDIS_APPENDFSYNC=everysec
REDIS_MAXMEMORY_POLICY=noeviction
REDIS_VOLUME_NAME=neomango_redis_data
```

정책:

- Redis는 외부 인터넷에 노출하지 않는다.
- 같은 Docker network 또는 localhost 바인딩만 허용한다.
- `0.0.0.0:6379` 공개 노출은 금지한다.
- AOF는 활성화하고 `appendfsync everysec`를 사용한다.
- Docker named volume을 사용한다.
- `maxmemory-policy noeviction`을 사용한다.
- Docker Compose 작성과 volume backup/restore 절차는 Phase 8-5 이후 범위다.

## 6. JWT

```text
JWT_SECRET=<jwt-secret-at-least-32-bytes>
JWT_ACCESS_TOKEN_VALIDITY_SECONDS=1800
JWT_REFRESH_TOKEN_VALIDITY_SECONDS=1209600
```

Refresh Token Redis TTL은 `JWT_REFRESH_TOKEN_VALIDITY_SECONDS`와 동일하게 저장한다. Redis TTL 없이 저장하지 않고, JWT refresh token 만료 시간보다 Redis TTL을 길게 두지 않는다.

## 7. Admin bootstrap

```text
ADMIN_BOOTSTRAP_ENABLED=false
ADMIN_EMAIL=<admin-email>
ADMIN_PASSWORD=<admin-initial-password>
ADMIN_NICKNAME=<admin-nickname>
```

초기 ADMIN은 one-time runner 또는 command 방식으로 생성한다. 일반 회원가입 API에서 `role=ADMIN` 주입은 허용하지 않는다.

## 8. 금지 사항

- 실제 운영 DB password 기록 금지
- 실제 Redis password 기록 금지
- 실제 JWT secret 기록 금지
- AWS credential 기록 금지
- ADMIN 실제 초기 비밀번호 기록 금지
