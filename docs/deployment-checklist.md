# Deployment Checklist

## Phase 9

### DB

- [ ] 운영 DB 백업 완료
- [ ] prodlike에서 Flyway V2 migration 검증 완료
- [ ] `users.login_id` backfill SQL 검증 완료
- [ ] `login_id` unique 제약 확인
- [ ] nickname unique 제약 확인
- [ ] `login_id`/nickname case-sensitive collation 확인

### 환경변수

- [ ] `ADMIN_BOOTSTRAP_LOGIN_ID` 설정 확인
- [ ] `ADMIN_BOOTSTRAP_EMAIL` 설정 확인
- [ ] `ADMIN_BOOTSTRAP_PASSWORD` 설정 확인
- [ ] `ADMIN_BOOTSTRAP_NICKNAME` 설정 확인
- [ ] 운영 `.env.prod` Git 미포함 확인
- [ ] `application-prod.yml`에 secret 하드코딩 없음 확인
- [ ] local profile이 운영 DB/Redis를 바라보지 않음 확인

### 인증

- [ ] 신규 회원가입 성공
- [ ] loginId 로그인 성공
- [ ] email 로그인 불가 확인
- [ ] refresh token 재발급 정상
- [ ] 관리자 로그인 정상

### SSE

- [ ] 알림 시간 KST 표시
- [ ] 비로그인 SSE 접근 시 401
- [ ] 만료 토큰 SSE 접근 시 401
- [ ] `text/event-stream` 응답에 JSON ErrorResponse 미작성
- [ ] 클라이언트 연결 종료 시 불필요한 WARN stack trace 없음
