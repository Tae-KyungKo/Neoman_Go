# 너만고 서비스 구조 문서 (`serviceArchitecture.md`)

> 이 문서는 너만고 플랫폼의 전체 서비스 구조, 사용자 흐름, 카테고리 단위 도메인 경계, 알림 구조를 설명한다.  
> `policy.md`가 도메인 정책과 예외 규칙을 정의한다면, 이 문서는 서비스가 사용자 관점에서 어떻게 동작하는지를 정의한다.  
> 구현 중 구조적 판단이 필요할 경우, 이 문서와 `policy.md`를 함께 참고한다.

---

## 1. 서비스 개요

너만고는 대학생이 게임, 스포츠 등 특정 활동 카테고리 안에서 팀을 만들고, 팀원을 모집하고, 매칭을 신청하고, 커뮤니티 게시판을 이용할 수 있는 웹 서비스다.

서비스의 핵심 구조는 다음과 같다.

```text
메인 페이지
→ 카테고리 선택
→ 카테고리 전용 페이지
   → 팀 목록 / 팀 생성 / 팀 가입 신청
   → 자유게시판
   → 매칭 보드
   → 카테고리 기반 활동 흐름
```

예시 카테고리:

```text
- 리그오브레전드
- 발로란트
- 배틀그라운드
- 피파
- 풋살
- 축구
- 농구
```

각 카테고리는 독립적인 서비스 공간처럼 동작한다.

예를 들어 사용자가 `리그오브레전드` 카테고리에 들어가면 다음 기능은 모두 `리그오브레전드` 범위 안에서 동작한다.

```text
- 리그오브레전드 팀 목록
- 리그오브레전드 팀 생성
- 리그오브레전드 팀 가입 신청
- 리그오브레전드 자유게시판
- 리그오브레전드 매칭 보드
```

`리그오브레전드`의 팀, 게시글, 매칭은 `풋살`, `농구`, `발로란트` 등 다른 카테고리의 데이터와 섞이면 안 된다.

---

## 2. 카테고리의 의미

너만고에서 `category`는 단순 검색 필터가 아니라 서비스 공간을 나누는 기준이다.

즉, `category`는 다음 도메인에서 공통 기준으로 사용된다.

```text
Team.category
Post.category
Match.category
UserCategoryMembership.category
Notification.category
```

카테고리는 다음 기능을 분리한다.

```text
- 팀 목록
- 팀 생성
- 팀 가입 신청
- 자유게시판
- 매칭 보드
```

단, 알림함은 카테고리별로 분리하지 않는다. 알림 구조는 회원 단위 단일 알림함을 사용한다.

---

## 3. 사용자 유형별 접근 범위

### 3.1 비회원

비회원은 조회 중심 기능만 사용할 수 있다.

가능한 기능:

```text
- 메인 페이지 접근
- 카테고리 선택
- 카테고리별 팀 목록 조회
- 카테고리별 자유게시판 게시글 목록 조회
- 게시글 상세 조회
- 카테고리별 매칭 보드 조회
```

불가능한 기능:

```text
- 팀 생성
- 팀 가입 신청
- 게시글 작성
- 댓글 작성
- 게시글 수정/삭제
- 댓글 수정/삭제
- 매칭 생성
- 매칭 신청
```

비회원이 작성, 신청, 생성 계열 기능을 시도하면 로그인 요구 응답을 반환해야 한다.

---

### 3.2 로그인 회원

로그인 회원은 카테고리별 참여 기능을 사용할 수 있다.

가능한 기능:

```text
- 팀 생성
- 팀 가입 신청
- 게시글 작성
- 댓글 작성
- 본인 게시글 수정/삭제
- 본인 댓글 수정/삭제
- 매칭 생성
- 매칭 신청
```

단, 팀 관리 기능은 팀 내 권한에 따라 제한된다.

---

## 4. 카테고리별 팀 구조

### 4.1 팀 생성

로그인 회원은 특정 카테고리에서 팀을 생성할 수 있다.

예를 들어 사용자 A가 `축구` 카테고리에서 팀을 생성하면 다음 데이터가 생성된다.

```text
Team.category = 축구
TeamMember.user = A
TeamMember.role = OWNER
TeamMember.status = ACTIVE
UserCategoryMembership.user = A
UserCategoryMembership.category = 축구
UserCategoryMembership.team = 생성된 팀
```

팀 생성자는 자동으로 해당 팀의 `OWNER`가 된다.

---

### 4.2 카테고리별 소속 제한

한 회원은 각 카테고리에서 하나의 팀에만 소속될 수 있다.

가능한 예:

```text
사용자 A
- 리그오브레전드 팀 1개 소속
- 풋살 팀 1개 소속
- 농구 팀 1개 소속
```

불가능한 예:

```text
사용자 A
- 리그오브레전드 팀 Alpha 소속
- 리그오브레전드 팀 Beta 동시 소속
```

즉, 같은 카테고리에서는 하나의 팀에만 소속될 수 있고, 서로 다른 카테고리에서는 각각 하나의 팀에 소속될 수 있다.

---

### 4.3 UserCategoryMembership의 역할

`UserCategoryMembership`은 팀원 목록 조회용 테이블이 아니다.

이 테이블의 목적은 다음과 같다.

```text
사용자가 특정 카테고리에서 이미 어떤 팀에 소속되어 있는지 기록하고,
같은 사용자 + 같은 카테고리 중복 소속을 DB 레벨에서 막는다.
```

역할 분리:

```text
TeamMember
- 특정 팀 내부의 멤버십을 표현한다.
- 역할 OWNER / MEMBER를 가진다.
- 상태 ACTIVE / INACTIVE를 가진다.

UserCategoryMembership
- 사용자-카테고리 점유 상태를 표현한다.
- 한 사용자가 같은 카테고리에서 하나의 팀에만 소속되도록 제약한다.
- unique(user_id, category) 제약으로 중복 소속을 방어한다.
```

예시:

```text
사용자 A가 리그오브레전드 팀 Alpha에 가입 승인됨

TeamMember:
- 사용자 A는 팀 Alpha의 MEMBER

UserCategoryMembership:
- 사용자 A는 리그오브레전드 카테고리에서 팀 Alpha를 점유 중
```

---

## 5. 팀 가입 신청 흐름

회원은 같은 카테고리 안에서 여러 팀에 가입 신청을 보낼 수 있다.

예:

```text
사용자 A가 리그오브레전드 카테고리에서
- Alpha 팀 신청: PENDING
- Beta 팀 신청: PENDING
- Gamma 팀 신청: PENDING
```

이후 Alpha 팀 OWNER가 신청을 승인하면 다음과 같이 처리된다.

```text
Alpha 신청: APPROVED
Beta 신청: CANCELED
Gamma 신청: CANCELED
사용자 A는 리그오브레전드 카테고리에서 Alpha 팀 소속
```

승인 시 생성되는 데이터:

```text
TeamMember
UserCategoryMembership
```

중복 소속 방어:

```text
- 같은 팀 중복 가입: TeamMember unique(team_id, user_id)
- 같은 카테고리 중복 소속: UserCategoryMembership unique(user_id, category)
```

---

## 6. 팀 권한 구조

팀 내 역할은 다음과 같다.

```text
OWNER
MEMBER
```

### 6.1 OWNER 권한

OWNER는 해당 팀에서 다음 기능을 수행할 수 있다.

```text
- 가입 신청 승인
- 가입 신청 거절
- 팀원 강퇴
- 주장 권한 위임
- 팀 마감
- 정책에 따른 팀 삭제
```

OWNER 권한은 해당 팀 안에서만 유효하다.

다른 팀의 OWNER라고 해서 현재 팀을 관리할 수 없다.

---

### 6.2 MEMBER 권한

MEMBER는 다음 기능을 수행할 수 있다.

```text
- 팀원 목록 조회
- 본인 팀 탈퇴
- 팀 내부 기능 이용
```

MEMBER는 다음 기능을 수행할 수 없다.

```text
- 가입 신청 승인
- 가입 신청 거절
- 다른 팀원 강퇴
- 주장 권한 위임
```

---

## 7. 팀원 관리 흐름

### 7.1 일반 MEMBER 탈퇴

일반 MEMBER가 팀에서 탈퇴하면 다음과 같이 처리한다.

```text
TeamMember.status = INACTIVE
해당 팀 카테고리의 UserCategoryMembership 삭제
```

TeamMember는 물리 삭제하지 않는다.

탈퇴 후 사용자는 같은 카테고리의 다른 팀에 다시 가입 신청할 수 있다.

---

### 7.2 팀원 강퇴

OWNER가 같은 팀의 ACTIVE MEMBER를 강퇴하면 다음과 같이 처리한다.

```text
대상 TeamMember.status = INACTIVE
대상 사용자의 해당 팀 카테고리 UserCategoryMembership 삭제
```

강퇴와 탈퇴는 상태적으로 구분하지 않는다.

강퇴된 사용자는 이후 해당 팀에 재신청할 수 있다.

---

### 7.3 OWNER 위임

OWNER는 같은 팀의 ACTIVE MEMBER에게 주장 권한을 위임할 수 있다.

처리 결과:

```text
기존 OWNER → MEMBER
대상 MEMBER → OWNER
```

한 팀에는 ACTIVE OWNER가 반드시 한 명만 존재해야 한다.

OWNER 위임 이력은 별도로 저장하지 않는다.

---

### 7.4 OWNER 탈퇴

#### 다른 ACTIVE MEMBER가 있는 경우

OWNER는 바로 탈퇴할 수 없다.

먼저 다른 ACTIVE MEMBER에게 OWNER를 위임해야 한다.

#### OWNER 혼자 남은 경우

OWNER가 혼자 남은 팀에서 탈퇴하면 팀은 자동 삭제된다.

처리 결과:

```text
Team.status = DELETED
OWNER TeamMember.status = INACTIVE
해당 팀 카테고리의 UserCategoryMembership 삭제
해당 팀의 PENDING 가입 신청 = REJECTED
```

Team과 TeamMember는 물리 삭제하지 않는다.

---

## 8. 카테고리별 자유게시판 구조

각 카테고리에는 독립된 자유게시판이 존재한다.

예:

```text
- 리그오브레전드 자유게시판
- 풋살 자유게시판
- 농구 자유게시판
```

Post는 반드시 하나의 category를 가진다.

```text
Post.category = 리그오브레전드
```

댓글은 게시글에 종속된다.

```text
Comment → Post
Post → category
```

Comment 자체에 category를 중복 저장할 필요는 없다.

---

### 8.1 비회원 게시판 접근

비회원은 다음 기능을 사용할 수 있다.

```text
- 카테고리별 게시글 목록 조회
- 게시글 상세 조회
```

비회원은 다음 기능을 사용할 수 없다.

```text
- 게시글 작성
- 댓글 작성
- 게시글 수정/삭제
- 댓글 수정/삭제
```

---

### 8.2 로그인 회원 게시판 접근

로그인 회원은 다음 기능을 사용할 수 있다.

```text
- 카테고리별 게시글 작성
- 게시글 상세 조회
- 본인 게시글 수정/삭제
- 댓글 작성
- 본인 댓글 수정/삭제
```

작성자 검증:

```text
게시글 수정/삭제는 작성자 본인만 가능
댓글 수정/삭제는 작성자 본인만 가능
```

삭제 정책:

```text
게시글은 Soft Delete 처리한다.
댓글은 Soft Delete 처리한다.
```

탈퇴 회원 표시:

```text
작성자가 탈퇴한 경우 닉네임은 익명으로 표시한다.
```

---

## 9. 카테고리별 매칭 보드 구조

각 카테고리에는 독립된 매칭 보드가 존재한다.

예:

```text
- 리그오브레전드 매칭 보드
- 풋살 매칭 보드
- 농구 매칭 보드
```

Match는 반드시 하나의 category를 가진다.

```text
Match.category = 풋살
```

비회원은 매칭 보드를 조회할 수 있다.

```text
- 매칭 목록 조회
- 매칭 상세 조회
```

비회원은 매칭에 참여할 수 없다.

```text
- 매칭 생성 불가
- 매칭 신청 불가
```

로그인 회원은 정책에 따라 매칭 생성/신청을 수행할 수 있다.

매칭 기능은 후순위 Phase에서 구체화한다.

---

## 10. 알림 구조

알림함은 카테고리별로 존재하지 않는다.

알림함은 회원 단위로 하나만 존재한다.

즉, 한 회원에게 발생한 모든 알림은 하나의 알림함으로 모인다.

예:

```text
읽지 않은 알림 - [리그오브레전드] 새로운 멤버 가입
읽지 않은 알림 - [풋살] 가입 승인
읽은 알림 - [발로란트] 가입 거절
```

알림은 어떤 카테고리에서 어떤 이벤트로 발생했는지 알 수 있어야 한다.

Notification은 최소한 다음 컨텍스트를 가질 수 있다.

```text
recipientUser
category
eventType
message
read 여부
createdAt
relatedResourceType
relatedResourceId
```

기본 알림 조회는 사용자 기준이다.

권장 API 구조:

```text
GET /api/notifications
GET /api/notifications/unread
PATCH /api/notifications/{notificationId}/read
```

카테고리별 필터가 필요하면 선택적으로 제공할 수 있다.

```text
GET /api/notifications?category=리그오브레전드
```

하지만 다음과 같은 구조는 사용하지 않는다.

```text
GET /api/categories/{category}/notifications
```

이유:

```text
알림함은 카테고리별 공간이 아니라 회원 단위 공간이기 때문이다.
```

---

## 11. API 설계 방향

카테고리별 리소스는 category를 경로 또는 쿼리로 명확히 받아야 한다.

권장 예시:

```text
GET /api/categories/{category}/teams
GET /api/categories/{category}/posts
GET /api/categories/{category}/matches
```

팀 내부 리소스는 teamId 기준으로 처리한다.

```text
GET /api/teams/{teamId}/members
POST /api/teams/{teamId}/members/me/leave
POST /api/teams/{teamId}/members/{teamMemberId}/kick
POST /api/teams/{teamId}/owner/delegate
```

회원 단일 공간 리소스는 user 기준으로 처리한다.

```text
GET /api/notifications
GET /api/my/applications
GET /api/my/teams
```

---

## 12. 구현 시 주의사항

### 12.1 카테고리 경계

카테고리별 독립 기능에서는 category 조건이 누락되면 안 된다.

예:

```text
리그오브레전드 게시판 목록에서 풋살 게시글이 조회되면 안 된다.
리그오브레전드 매칭 보드에서 축구 매칭이 조회되면 안 된다.
```

---

### 12.2 UserCategoryMembership 사용 범위

UserCategoryMembership은 다음에 사용한다.

```text
- 팀 생성 시 카테고리 점유 생성
- 팀 가입 승인 시 카테고리 점유 생성
- 팀 탈퇴/강퇴/팀 삭제 시 카테고리 점유 삭제
- 같은 카테고리 중복 소속 방어
```

UserCategoryMembership은 다음에 사용하지 않는다.

```text
- 팀원 목록 조회
- 게시글 조회
- 댓글 조회
- 매칭 보드 조회
- 알림함 조회
```

---

### 12.3 조회와 참여의 권한 분리

비회원은 여러 카테고리 리소스를 조회할 수 있다.

하지만 생성, 신청, 작성, 수정, 삭제와 같은 참여 기능은 로그인 사용자만 가능하다.

```text
조회 = 비회원 가능
참여 = 로그인 필요
```

단, 수정/삭제는 로그인만으로 부족하며 작성자 또는 권한자 검증이 필요하다.

---

### 12.4 점진적 확장 기준

초기 구현에서는 다음 기능을 제외한다.

```text
- 게시글 좋아요
- 대댓글
- 이미지 첨부
- 신고
- 인기글
- 검색
- 해시태그
- 알림 실시간 전송 고도화
- 감사 로그
- 위임 이력
```

이 기능들은 필요성이 생기면 후속 Phase에서 추가한다.
