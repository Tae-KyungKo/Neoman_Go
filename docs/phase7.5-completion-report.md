# Phase 7.5 Completion Report

## 1. 결론

Phase 7.5 종료 시점의 너만고 프로젝트는 백엔드 중심의 로컬 개발 상태로 정리한다.

- 인증/인가: JWT Access Token / Refresh Token 구조
- Refresh Token 저장소: Redis 사용
- 핵심 도메인: User, Team, TeamMember, TeamApplication, Post, Comment, Notice, Notification
- 알림: DB Notification 저장 + SSE 실시간 전송 보조 채널
- SSE endpoint: `GET /api/notifications/stream`
- 테스트: 주요 도메인 서비스/컨트롤러/알림/SSE 테스트가 존재함

이번 문서는 운영 배포 완료 선언이 아니다. Phase 8 배포 준비 전에 현재 상태와 위험을 고정하기 위한 기준 문서다.

## 2. 현재 프로젝트 상태

확인 기준: 2026-06-09, 브랜치 `feat/UI-first-deploy`

- 워킹트리: 작업 시작 전 깨끗한 상태
- 백엔드: Spring Boot 기반 단일 애플리케이션
- 프론트엔드: 현재 저장소에서 별도 프론트엔드 디렉터리는 확인되지 않음
- CI/CD: `.github/pull_request_template.md`는 있으나 GitHub Actions workflow는 확인되지 않음
- 배포 코드: Dockerfile, Nginx 설정, Flyway migration, 운영 profile은 아직 구현 범위 밖
- 환경 예시: `.env.example` 파일은 현재 확인되지 않음

## 3. Phase 7.5 완료 범위

### 인증/인가

- Spring Security 기반 stateless 인증 구조를 사용한다.
- `/api/auth/login`, `/api/auth/signup`, `/api/auth/reissue`는 공개 API로 설정되어 있다.
- 관리자 API는 `/api/admin/**` 경로에서 `ADMIN` role을 요구한다.
- 비밀번호 인코더는 BCrypt 기반이다.

### 팀 도메인

- 팀 생성, 팀원, 가입 신청, OWNER 권한, 팀 탈퇴/강퇴/위임 관련 구조가 존재한다.
- 동시성 테스트와 repository 테스트가 존재한다.
- 중복 가입 및 카테고리 단위 소속 제약을 고려한 repository/entity 구조가 존재한다.

### 게시글/댓글/공지

- Post, Comment, Notice 도메인과 컨트롤러/서비스/DTO가 존재한다.
- 일반 사용자 조회와 관리자 공지 관리 API가 분리되어 있다.

### 알림/SSE

- Notification 저장소와 조회/읽음 처리 API가 존재한다.
- `NotificationCreatedEvent`와 AFTER_COMMIT 기반 SSE 전송 흐름이 존재한다.
- `NotificationSseService`는 현재 메모리 기반 `ConcurrentHashMap<Long, Set<SseEmitter>>`로 connection을 관리한다.
- SSE 연결 URL은 운영 기준으로 `https://api.neomango.kr/api/notifications/stream`로 확정한다.

## 4. 운영 배포 전 확인된 위험

현재 `src/main/resources/application.yml`에는 다음 운영 차단 항목이 있다.

- `spring.jpa.hibernate.ddl-auto: create`
- `spring.profiles.include: secret`
- Redis host가 `localhost`로 고정
- datasource URL/username/password는 secret profile 의존

현재 `SecurityConfig`에는 다음 운영 차단 항목이 있다.

- CORS 허용 origin이 `http://localhost:5173`만 허용
- 운영 프론트 URL인 `https://neomango.kr`, `https://www.neomango.kr`가 아직 반영되지 않음
- CORS 설정이 환경변수가 아니라 코드에 하드코딩되어 있음

현재 `build.gradle`에는 다음 운영 검토 항목이 있다.

- `spring-boot-devtools`가 `implementation` scope로 포함되어 있음
- `p6spy-spring-boot-starter`가 `implementation` scope로 포함되어 있음
- 운영 build에서 devtools/p6spy가 포함되지 않도록 scope 또는 profile 전략을 재검토해야 함

현재 SSE 구현에는 다음 운영 검토 항목이 있다.

- connection 저장소가 애플리케이션 메모리 기반이라 다중 EC2 인스턴스에서 인스턴스 간 이벤트 전달이 되지 않음
- Nginx/ALB/CloudFront 경유 시 buffering, idle timeout, keep-alive 정책이 별도 검증되어야 함
- 기본 브라우저 `EventSource`는 Authorization header를 직접 넣기 어렵기 때문에 프론트 구현 방식이 고정되어야 함

## 5. 완료 판정

Phase 7.5는 기능 구현 관점에서는 로컬 개발 상태 종료로 본다.

다만 운영 배포 가능 상태는 아니다. Phase 8에서는 배포 인프라, prod/prodlike profile, DB schema 관리, CI 검증, CORS/SSE 운영 도메인 반영을 먼저 처리해야 한다.

