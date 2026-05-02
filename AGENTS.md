# 너만고 Codex Instructions

## Project Context
너만고는 대학생이 게임/스포츠 팀원을 모집하고 팀 가입을 신청하는 온라인 매칭 플랫폼이다.

핵심 도메인:
- User
- Team
- TeamMember
- TeamApplication
- Match
- Notification
- Post
- Comment
- Admin

## Backend Stack
- Java 17
- Spring Boot
- Spring Security
- JPA/Hibernate
- MySQL
- Redis
- JWT Access/Refresh Token
- S3 for file upload

## Mandatory Architecture Rules
- Controller는 요청/응답 DTO 변환만 담당한다.
- 핵심 비즈니스 로직은 Service/Application Layer에 둔다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- 모든 쓰기 작업은 트랜잭션 경계를 명확히 둔다.
- Soft Delete 정책을 고려한다.
- 팀 가입 승인, 마지막 자리 경쟁 등 동시성 이슈는 반드시 처리한다.
- N+1이 발생할 수 있는 조회는 fetch join, EntityGraph, DTO projection 중 하나로 해결한다.

## Security Rules
- 인증은 JWT Access Token + Refresh Token 구조를 기본으로 한다.
- Refresh Token은 Redis에 저장한다.
- 비밀번호는 BCrypt로 단방향 해시한다.
- 사용자 권한 검증은 SecurityContext와 도메인 권한 검증을 함께 사용한다.
- 팀장 권한이 필요한 API는 TeamMember role을 기준으로 검증한다.

## Domain Policy
- 팀 생성자는 자동으로 OWNER 역할의 TeamMember가 된다.
- OWNER는 권한 위임 없이 팀 탈퇴할 수 없다.
- 팀 가입 신청은 중복 신청을 방지해야 한다.
- 팀 가입 승인 시 팀 정원 초과를 막아야 한다.
- 탈퇴 회원은 Soft Delete 처리한다.

## Package Naming Convention
- `controller`: HTTP 요청/응답 처리 계층
- `service`: 비즈니스 유스케이스와 트랜잭션 경계 담당
- `entity`: JPA Entity와 도메인 상태 변경 메서드 위치
- `repository`: DB 접근 계층
- `dto`: 요청/응답 DTO

주의:
- `entity` 패키지명을 사용하더라도 Entity를 단순 데이터 컨테이너로 취급하지 않는다.
- 핵심 상태 변경은 setter가 아니라 도메인 메서드로 표현한다.
- Controller에는 비즈니스 로직을 두지 않는다.
- Service는 트랜잭션 경계와 유스케이스 조합을 담당한다.

## Engineering Principles
- Entity를 API 응답으로 직접 반환하지 않는다.
- 모든 쓰기 작업은 트랜잭션 경계를 가진다.
- 비즈니스 규칙은 서비스 또는 도메인 계층에 위치한다.
- 동시성 문제를 고려한다.
- 인증과 권한 검증을 분리한다.

## Current Technical Decisions
- JWT Access/Refresh 구조를 사용한다.
- Refresh Token은 Redis에 저장한다.
- 실시간 알림은 SSE 기반으로 우선 구현한다.
- Querydsl을 조회 최적화에 사용한다.

## Future Considerations
- SSE → WebSocket 전환 가능성
- CQRS 분리 가능성
- Kafka/Event Driven Architecture 확장 가능성

## Coding Rules
- Lombok은 필요한 범위에서만 사용한다.
- Entity에는 무분별한 setter를 만들지 않는다.
- 생성/변경은 정적 팩토리 메서드 또는 도메인 메서드를 사용한다.
- 예외는 커스텀 예외와 ErrorCode 기반으로 관리한다.
- 테스트 가능한 구조로 작성한다.

## Before Coding
- 요구사항이 불명확하면 먼저 질문한다.
- 큰 기능은 구현 전에 설계안을 먼저 제시한다.
- DB 정합성, 트랜잭션, 동시성, 보안 영향을 먼저 검토한다.
- AGENTS.md 파일의 내용과 사용자의 질문에 충돌이 있다면 먼저 질문한다.
- 기술에 대한 트레이드 오프를 고려한다.

## After Coding
- 변경 파일 목록을 요약한다.
- 잠재적 위험 요소를 설명한다.
- 필요한 테스트 케이스를 제안한다.
- 작성한 코드에 대한 트레이드 오프를 설명한다.