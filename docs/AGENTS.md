# AGENTS.md

# 너만고 Codex Instructions

---

# 1. Project Context

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

핵심 요구사항:
- JWT 기반 인증/인가
- 팀 가입 신청 및 승인 프로세스
- 중복 가입 신청 및 중복 승인 방지
- 팀장 권한 위임 정책
- Soft Delete 정책
- 실시간 알림(SSE 우선)
- 확장 가능한 구조

클라이언트 확장 방향:
- 너만고는 개발이 어느 정도 완료된 뒤 React 기반 웹 프론트엔드와 모바일 앱으로 확장할 것을 염두에 둔다.
- 백엔드는 서버 렌더링 페이지를 반환하기보다 REST API 중심으로 설계한다.
- Controller는 HTML/View 반환이 아니라 요청 검증, 인증 사용자 식별, DTO 기반 JSON 응답 반환에 집중한다.
- React 웹, 모바일 앱 등 여러 클라이언트가 동일한 API를 사용할 수 있도록 API 계약의 일관성을 유지한다.

---

# 2. Behavioral Guidelines

이 문서는 LLM 기반 코딩 에이전트의 흔한 실수를 줄이기 위한 행동 규칙이다.

기본 원칙:
- 속도보다 정확성을 우선한다.
- 추측하지 않는다.
- 요구사항 범위를 임의로 확장하지 않는다.
- 불확실하면 질문한다.
- 최소 수정 원칙을 따른다.

---

# 3. Think Before Coding

구현 전에 반드시 다음을 검토한다.

## Mandatory Rules

- 요구사항이 불명확하면 먼저 질문한다.
- 여러 해석이 가능하면 임의로 선택하지 말고 설명한다.
- 더 단순한 해결책이 있다면 제안한다.
- 위험하거나 과도한 설계라면 이유를 설명하고 대안을 제시한다.
- DB 정합성, 트랜잭션, 동시성, 보안 영향을 먼저 검토한다.
- AGENTS.md 규칙과 사용자 요청이 충돌하면 구현 전에 질문한다.
- 큰 기능은 구현 전에 설계안을 먼저 제시한다.
- 기술 선택 시 Trade-off를 설명한다.

## Examples

좋은 행동:
- "동시성 제어는 비관락과 원자적 UPDATE 방식이 가능합니다. 현재 요구사항 기준으로는 원자적 UPDATE가 더 단순합니다."

나쁜 행동:
- 질문 없이 임의로 Redis Lock 추가
- CQRS/Kafka를 요구사항 없이 선반영
- 추상화 과잉 설계

---

# 4. Simplicity First

최소 코드로 요구사항을 해결한다.

## Mandatory Rules

- 요청받지 않은 기능 추가 금지
- 단일 사용 코드를 위한 추상화 금지
- 미래 확장을 이유로 복잡성 추가 금지
- 불필요한 설정화/유연화 금지
- 존재 가능성이 낮은 예외 시나리오 과잉 처리 금지
- 코드가 과도하게 커졌다면 단순화 검토
- 실무에서 유지보수 가능한 수준을 유지

## Architecture Philosophy

좋은 코드:
- 명확한 책임
- 짧은 흐름
- 예측 가능한 구조
- 단순한 트랜잭션 경계

나쁜 코드:
- 과도한 Generic
- 과도한 추상화 계층
- 불필요한 Design Pattern 남용
- "확장 가능성"만을 위한 구조

---

# 5. Surgical Changes

필요한 부분만 수정한다.

## Mandatory Rules

- 사용자 요청과 직접 관련된 코드만 수정한다.
- 관련 없는 리팩토링 금지
- 기존 코드 스타일을 존중한다.
- 불필요한 포맷 변경 금지
- 기존 dead code는 임의 삭제하지 않는다.
- 자신의 수정으로 발생한 unused import/variable만 제거한다.
- 모든 변경 라인은 사용자 요구사항과 연결 가능해야 한다.

## Examples

허용:
- 현재 기능 구현에 필요한 DTO 추가
- 현재 기능에 필요한 Repository 메서드 추가

비허용:
- unrelated package rename
- unrelated formatter 적용
- unrelated naming convention 수정

---

# 6. Goal-Driven Execution

모든 작업은 검증 가능한 목표 기반으로 수행한다.

## Mandatory Rules

작업 전에:
1. 목표 정의
2. 구현 계획 제시
3. 검증 방법 정의

작업 후:
1. 테스트
2. 빌드 검증
3. 위험 요소 설명

## Example Workflow

1. Team 가입 승인 API 구현
    - verify:
        - 중복 승인 방지
        - OWNER 권한 검증

2. 테스트 작성
    - verify:
        - 동일 신청 중복 승인 테스트
        - 권한 없는 사용자 실패 테스트

3. API 응답 검증
    - verify:
        - Entity 직접 반환 여부 확인
        - ErrorCode 응답 일관성 확인

---

# 7. Backend Stack

- Java 17
- Spring Boot
- Spring Security
- JPA/Hibernate
- MySQL
- Redis
- JWT Access/Refresh Token
- AWS S3

---

# 8. Mandatory Architecture Rules

## Layer Rules

- Controller는 요청/응답 DTO 변환만 담당한다.
- 핵심 비즈니스 로직은 Service/Application Layer에 둔다.
- Repository는 DB 접근만 담당한다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- 비즈니스 규칙은 서비스 또는 도메인 계층에 위치한다.

## Transaction Rules

- 모든 쓰기 작업은 트랜잭션 경계를 명확히 둔다.
- 읽기 전용 조회는 readOnly=true를 우선 검토한다.
- 트랜잭션 범위를 불필요하게 넓히지 않는다.

## JPA Rules

- N+1 발생 가능성을 항상 검토한다.
- fetch join, EntityGraph, DTO projection 중 하나로 해결한다.
- 무분별한 EAGER 사용 금지
- Dirty Checking을 우선 활용한다.
- Cascade는 Aggregate 생명주기가 명확할 때만 사용한다.
- orphanRemoval은 실제 Aggregate 삭제 정책과 일치할 때만 사용한다.

## API Rules

- 공통 응답 포맷 사용
- 예외는 ErrorCode 기반으로 관리
- Validation 실패 응답 일관성 유지
- Entity 직접 반환 금지

---

# 9. Security Rules

- 인증은 JWT Access Token + Refresh Token 구조를 기본으로 한다.
- Refresh Token은 Redis에 저장한다.
- 비밀번호는 BCrypt로 단방향 해시한다.
- 사용자 권한 검증은 SecurityContext + 도메인 권한 검증을 함께 사용한다.
- 팀장 권한 API는 TeamMember role 기준으로 검증한다.
- 인증(Authentication)과 인가(Authorization)를 분리한다.
- 민감 정보 로그 출력 금지
- Refresh Token은 DB 저장보다 Redis 저장을 우선한다.

---

# 10. Domain Policy

## Team Policy

- 팀 생성자는 자동으로 OWNER 역할의 TeamMember가 된다.
- OWNER는 권한 위임 없이 팀 탈퇴 불가
- OWNER 변경은 명시적 권한 위임으로 처리
- 팀에는 최대 허용 멤버 수를 두지 않는다.
- 팀 가입 신청 중복 방지 필수

## User Policy

- 탈퇴 회원은 Soft Delete 처리
- 탈퇴 후에도 데이터 정합성 유지
- 작성 게시글 정책 고려

## Concurrency Policy

반드시 동시성을 고려해야 하는 기능:
- 중복 신청 방지
- 중복 승인 방지
- 동일 사용자의 중복 TeamMember 생성 방지

필요 시:
- 비관락
- 낙관락
- 원자적 UPDATE
- Redis 분산락

중 하나를 상황에 맞게 선택한다.

Trade-off를 설명한다.

---

# 11. Package Naming Convention

- `controller`
    - HTTP 요청/응답 처리

- `service`
    - 유스케이스
    - 트랜잭션 경계
    - 도메인 orchestration

- `entity`
    - JPA Entity
    - 도메인 상태 변경 메서드

- `repository`
    - DB 접근 계층

- `dto`
    - Request/Response DTO

## Important Notes

- entity 패키지를 사용하더라도 Anemic Model로 만들지 않는다.
- 상태 변경은 setter보다 도메인 메서드를 우선한다.
- Controller에 비즈니스 로직 금지
- Service는 orchestration 역할 수행

---

# 12. Coding Rules

## General Rules

- Lombok은 필요한 범위에서만 사용
- Entity에 무분별한 setter 금지
- 생성은 정적 팩토리 메서드 우선
- 상태 변경은 도메인 메서드 우선
- 테스트 가능한 구조 유지
- 의미 없는 util 클래스 남용 금지

## Exception Rules

- ErrorCode 기반 예외 사용
- RuntimeException 직접 남용 금지
- 예외 메시지 일관성 유지

## Naming Rules

- 이름은 도메인 의미를 반영
- 축약어 남용 금지
- Boolean은 is/has/can 계열 사용 권장

---


# 13. Future Considerations

현재 즉시 구현 대상은 아니지만 고려 가능한 방향:

- SSE → WebSocket 전환
- CQRS 분리
- Kafka/Event Driven Architecture
- MSA 분리 가능성
- 캐시 전략 고도화

주의:
- 미래 확장 가능성을 이유로 현재 복잡도를 과도하게 증가시키지 않는다.

---

# 14. Before Coding Checklist

구현 전 반드시 확인:

- 요구사항이 명확한가?
- DB 정합성 문제가 없는가?
- 트랜잭션 경계가 적절한가?
- 동시성 문제가 발생 가능한가?
- 권한 검증이 충분한가?
- Soft Delete 영향이 있는가?
- 기존 코드 스타일과 충돌하지 않는가?
- 더 단순한 방법은 없는가?

---

# 16. After Coding Checklist

구현 후 반드시 제공:

## Summary
- 변경 파일 목록
- 핵심 변경 내용

## Verification
- 수행한 테스트
- 검증 결과

## Risk
- 잠재적 위험 요소
- 운영 시 주의점

## Trade-off
- 선택한 구현 방식의 장단점
- 대안 비교

## Additional Suggestions
- 필요한 추가 테스트
- 추후 개선 가능 포인트
