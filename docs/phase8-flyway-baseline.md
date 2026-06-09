# Phase 8 Flyway Baseline

## 1. 결론

Phase 8-3부터 prodlike/prod DB schema는 Hibernate `ddl-auto`가 아니라 Flyway migration으로 관리한다.

정책:

- local/test에서는 Flyway를 비활성화한다.
- prodlike/prod에서만 Flyway를 활성화한다.
- prodlike/prod는 `ddl-auto=validate`를 유지한다.
- V1 migration은 현재 JPA Entity 기준 baseline DDL만 포함한다.
- V1 migration에는 seed data를 넣지 않는다.

## 2. 추가된 migration

```text
src/main/resources/db/migration/V1__baseline_schema.sql
```

V1 baseline 포함 테이블:

- `users`
- `teams`
- `team_members`
- `team_applications`
- `user_category_memberships`
- `posts`
- `comments`
- `notices`
- `audit_logs`
- `notifications`

V1 baseline 제외:

- match
- chat
- inquiry
- banner
- statistics
- 기타 미구현 기능 테이블

## 3. Seed Data 정책

V1 baseline에는 DDL만 포함한다.

포함하지 않는 것:

- ADMIN 계정 insert
- 샘플 user/team/post/comment 데이터
- 운영 seed 데이터

초기 ADMIN 생성은 Phase 8-8에서 one-time runner 또는 command 방식으로 처리한다. 일반 회원가입 API에서 `role=ADMIN` 주입은 허용하지 않는다.

## 4. Profile별 Flyway 정책

| profile | Flyway | ddl-auto | 목적 |
| --- | --- | --- | --- |
| local | disabled | update | 개발 편의성 유지 |
| test | disabled | create-drop | 기존 테스트 안정성 유지 |
| prodlike | enabled | validate | Flyway migration + schema 검증 |
| prod | enabled | validate | 운영 DB migration + schema 검증 |

`baselineOnMigrate=true`는 사용하지 않는다.

이유:

- 1차 배포는 빈 DB에 `V1__baseline_schema.sql`부터 적용하는 정책이다.
- 기존 운영 DB가 이미 존재하는 상황은 현재 전제하지 않는다.
- 기존 DB를 Flyway 관리 대상으로 편입해야 하는 경우에는 별도 전환 계획을 세워야 한다.

## 5. MySQL 기준

MySQL 8 기준으로 `utf8mb4`와 `utf8mb4_0900_ai_ci`를 사용한다.

모든 V1 테이블은 다음 기준을 따른다.

```sql
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
```

## 6. Index/Constraint 기준

V1 baseline은 Entity 제약과 현재 Repository 조회 패턴을 기준으로 최소한의 unique/index를 포함한다.

주요 제약:

- `users.email` unique
- `team_members(team_id, user_id)` unique
- `user_category_memberships(user_id, category)` unique

주요 인덱스:

- 팀 목록: `teams(category, deleted_at)`, `teams(status, deleted_at)`
- 팀 멤버: `team_members(team_id, status, joined_at, id)`, `team_members(team_id, role, status)`
- 팀 신청: `team_applications(team_id, status, created_at)`, `team_applications(applicant_id, created_at)`
- 게시글: `posts(category, status, created_at)`
- 댓글: `comments(post_id, status, created_at)`
- 공지: `notices(status, created_at)`
- 알림: `notifications(receiver_id, created_at)`, `notifications(receiver_id, read_at)`

## 7. Prodlike 검증 기준

Flyway 검증은 기존 local 개발 DB가 아니라 별도 `neomango_prodlike` DB를 기준으로 수행한다.

예시 환경변수:

```text
SPRING_PROFILES_ACTIVE=prodlike
DB_URL=jdbc:mysql://localhost:3306/neomango_prodlike?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
JWT_SECRET=<jwt-secret-at-least-32-bytes>
REDIS_HOST=localhost
REDIS_PORT=6379
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

실제 credential은 문서에 기록하지 않는다.

검증 목표:

- Flyway V1 migration 적용 성공
- Hibernate `ddl-auto=validate` 통과
- 애플리케이션 context 로딩 성공

## 8. Rollback 정책

Phase 8에서는 down migration 파일을 작성하지 않는다.

운영 migration 문제 발생 시 대응:

- forward fix migration
- 배포 전 DB snapshot/backup 복구

운영 DB migration 전에는 snapshot/backup을 남기는 것을 원칙으로 한다.

## 9. Phase 8-3.5 Prodlike Runtime 검증 결과

검증 일자: 2026-06-09

검증 환경:

- Docker MySQL 8
- MySQL version: 8.4.9
- 검증 DB: `neomango_prodlike`
- Redis: 로컬 검증용 `neomango-redis`
- 운영 credential: 사용하지 않음
- seed data: 없음

검증 결과:

- Flyway V1 migration 적용 성공
- `flyway_schema_history` 생성 확인
- `V1__baseline_schema.sql` success 상태 확인
- MVP 테이블 생성 확인
- Hibernate `ddl-auto=validate` 통과
- prodlike profile 애플리케이션 기동 성공

생성 확인 테이블:

- `users`
- `teams`
- `team_members`
- `team_applications`
- `user_category_memberships`
- `posts`
- `comments`
- `notices`
- `audit_logs`
- `notifications`
- `flyway_schema_history`

검증 중 확인한 warning:

- Flyway가 MySQL 8.4에 대해 "현재 Flyway 버전의 최신 테스트 지원 MySQL은 8.1"이라는 upgrade 권고 warning을 출력했다.
- migration과 Hibernate validate는 통과했지만, 운영 RDS MySQL minor version 선택 시 Flyway 지원 범위를 함께 확인한다.

초기 접속 이슈:

- 컨테이너 내부 root 계정만으로는 호스트에서 접속하는 root user가 거부되어 prodlike bootRun이 실패했다.
- 로컬 검증용 DB 사용자를 별도로 생성한 뒤 같은 세션의 환경변수로만 credential을 주입해 재검증했다.
- 해당 credential은 운영 credential이 아니며 문서에 기록하지 않는다.

