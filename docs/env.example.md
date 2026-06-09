# Environment Variable Example

## 1. 결론

이 문서는 Phase 8-2 기준 profile/env/secret 분리 구조에서 필요한 환경변수를 정리한다.

실제 운영 secret 값은 이 문서와 `.env.example`에 절대 기록하지 않는다. 모든 민감값은 placeholder로만 표현한다.

## 2. 도메인 기준

```text
FRONTEND_BASE_URL=https://neomango.kr
FRONTEND_WWW_URL=https://www.neomango.kr
BACKEND_API_URL=https://api.neomango.kr
SSE_URL=https://api.neomango.kr/api/notifications/stream
CORS_ALLOWED_ORIGINS=https://neomango.kr
```

`https://www.neomango.kr`는 root domain으로 redirect하는 정책이다. 다만 redirect 전 요청이나 운영 전환 중 예외가 있으면 `CORS_ALLOWED_ORIGINS`에 임시로 추가할 수 있다.

## 3. Spring profile

```text
SPRING_PROFILES_ACTIVE=prod
```

지원 profile:

- `local`
- `test`
- `prodlike`
- `prod`

`application-secret.yml`은 local 전용 secret 파일로 간주한다. prod/prodlike는 secret yml이 아니라 환경변수 기반으로 값을 주입한다.

## 4. Database

```text
DB_URL=jdbc:mysql://<host>:3306/<database>?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
```

prod/prodlike에서는 `ddl-auto=create/update`를 사용하지 않는다. Phase 8-2에서는 `validate`까지만 설정하고, 실제 Flyway baseline은 Phase 8-3에서 진행한다.

## 5. Redis

```text
REDIS_HOST=<redis-host>
REDIS_PORT=6379
REDIS_PASSWORD=<redis-password>
```

Phase 8 1차 운영 Redis는 EC2 Docker Redis + AOF 기준이다. 비밀번호, AOF, volume, backup/restore 세부 정책은 Phase 8-4에서 구체화한다.

## 6. JWT

```text
JWT_SECRET=<jwt-secret-at-least-32-bytes>
JWT_ACCESS_TOKEN_VALIDITY_SECONDS=1800
JWT_REFRESH_TOKEN_VALIDITY_SECONDS=1209600
```

prod/prodlike에서 `JWT_SECRET`이 없으면 애플리케이션이 insecure default로 조용히 실행되면 안 된다.

## 7. Admin bootstrap

```text
ADMIN_EMAIL=<admin-email>
ADMIN_PASSWORD=<admin-initial-password>
```

초기 ADMIN은 one-time runner 또는 command 방식으로 생성한다. 일반 회원가입 API에서 `role=ADMIN` 주입은 허용하지 않는다.

## 8. 금지사항

- 실제 운영 DB password 기록 금지
- 실제 Redis password 기록 금지
- 실제 JWT secret 기록 금지
- AWS credential 기록 금지
- ADMIN 실제 초기 비밀번호 기록 금지
