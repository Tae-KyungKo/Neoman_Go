# Phase 9. 운영 안정화 및 자체 로그인 방식 개선

## 목적

Phase 9는 운영 배포 이후 자체 로그인 체계를 email 기반에서 loginId 기반으로 전환하고, 회원가입/로그인 입력 정책, SSE 운영 안정성, 게시글/댓글 입력 제한, prodlike QA 기준을 정리하는 단계다.

Phase 9-0은 구현 단계가 아니다. Java, React, Flyway SQL, application 설정, env 파일은 수정하지 않고 Phase 9 착수를 위한 문서 기준선만 확정한다.

## 포함 범위

- 회원가입 입력 항목 변경
- 기존 email 기반 로그인에서 loginId 기반 로그인으로 변경
- 아이디/닉네임 중복 확인 API 추가 예정
- `users.login_id` 추가 예정
- 이메일 중복 제한 유지
- 아이디/닉네임 case-sensitive 정책 적용 예정
- 비밀번호 정책 보강
- SSE 알림 시간 KST 보정 예정
- SSE `text/event-stream` 예외 처리 분리 예정
- 게시글/댓글/계정 입력값 길이 제한 적용 예정
- 프론트 로그인/회원가입 화면 최소 수정 예정
- prodlike QA 및 운영 배포 체크리스트 보강

## 제외 범위

- OAuth 로그인
- 이메일 인증
- 아이디 찾기
- 비밀번호 찾기
- 운영 프론트 전체 리디자인
- 관리자 기능 확장
- 매치 기능
- 팀챗

## 확정 정책

### loginId

- DB 컬럼명: `login_id`
- Java 필드명/API 필드명: `loginId`
- 길이: 4~12자
- 허용 문자: 영어 대소문자, 숫자
- 특수문자와 공백 금지
- 한글 금지
- 정규식 기준: `^[A-Za-z0-9]{4,12}$`
- 대소문자 구분
- DB collation도 case-sensitive로 맞출 예정

### email

- 이메일은 로그인에 사용하지 않는다.
- 이메일 중복 제한은 유지한다.
- 이메일 인증은 이번 Phase에서 하지 않는다.

### nickname

- 길이: 2~12자
- 중복 제한
- 대소문자 구분
- 금칙어 검사는 대소문자를 무시한다.
- 금지 닉네임: `관리자`, `운영자`, `ADMIN`, `admin` 계열
- 관리자 계정의 실제 DB nickname은 `관리자`로 두지 않는다.
- 공지사항 작성자 표시는 기존처럼 `관리자`로 유지할 수 있다.

### password

- 길이: 8~16자
- 공백 문자 금지
- 허용 문자: 영문 대문자, 영문 소문자, 숫자, 일반 특수문자
- 한글 비밀번호는 허용하지 않는다.
- 권장 정규식 예시:

```text
^[A-Za-z0-9!@#$%^&*()_\-+=\[\]{};:'",.<>/?\\|`~]{8,16}$
```

### 게시글/댓글 길이

- 게시글 제목: 1~100자
- 게시글 본문: 1~5000자
- 댓글 본문: 1~1000자

## 작업 순서

1. 정책 상수화
2. Flyway migration 설계
3. User 엔티티 loginId 추가
4. UserRepository 수정
5. 회원가입 API 변경
6. 아이디/닉네임 중복 확인 API 추가
7. 로그인 API 변경
8. Refresh Token Redis key 영향 점검
9. ADMIN bootstrap loginId 대응
10. 프론트 로그인/회원가입 최소 수정
11. SSE 알림 시간 KST 보정
12. SSE 예외 처리 분리
13. 게시글/댓글 길이 제한 적용
14. 테스트 보강
15. prodlike QA
16. `release/v1.1.0` 운영 배포

## 운영 배포 주의사항

- 운영 DB/Redis 접속정보를 `application-prod.yml`에 직접 작성하지 않는다.
- 운영 도메인, secret, password를 feature 브랜치에 커밋하지 않는다.
- local profile에서 운영 DB/Redis를 바라보게 설정하지 않는다.
- prod/prodlike에서는 `application-secret.yml`을 import하지 않는다.
- 운영 `.env.prod`는 Git에 포함하지 않는다.
- 환경변수 예시는 `.env.example` 또는 `docs/env.example.md`에만 작성한다.
- 문서에는 환경변수 이름만 작성하고 실제 운영 값을 쓰지 않는다.
- Phase 9 이후 ADMIN bootstrap에는 다음 환경변수가 필요하다.
  - `ADMIN_BOOTSTRAP_LOGIN_ID`
  - `ADMIN_BOOTSTRAP_EMAIL`
  - `ADMIN_BOOTSTRAP_PASSWORD`
  - `ADMIN_BOOTSTRAP_NICKNAME`

## 완료 기준

- [ ] `docs/phase9-plan.md`에 Phase 9 목적, 범위, 정책, 작업 순서, 운영 배포 주의사항이 정리되어 있다.
- [ ] Auth API 계약 문서에 signup/login/check-login-id/check-nickname 계약이 정리되어 있다.
- [ ] `docs/policy.md`에 Phase 9 회원가입/로그인, 비밀번호, 닉네임, 이메일, 게시글/댓글 길이, 운영 환경 설정 파일 정책이 반영되어 있다.
- [ ] 배포 체크리스트에 Phase 9 DB, 환경변수, 인증, SSE 검증 항목이 포함되어 있다.
- [ ] troubleshooting 문서에 SSE `text/event-stream` 예외 처리 이슈가 기록되어 있다.
- [ ] Java/React/Flyway/application/env/build 파일 변경 없이 문서 변경만 수행한다.
- [ ] 실제 운영 secret, token, password, credential 값이 문서에 포함되지 않는다.
