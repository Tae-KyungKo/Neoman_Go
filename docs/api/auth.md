# Auth API Contract

## 공통 원칙

Phase 9 이후 로그인 식별자는 email이 아니라 `loginId`다. email은 로그인에 사용하지 않지만 중복 제한은 유지한다.

아이디/닉네임 중복 확인 API는 UX 보조 기능이다. 최종 중복 방어는 서버 검증과 DB unique constraint로 처리해야 한다.

## POST /api/auth/signup

회원가입 API다.

### Request

```json
{
  "loginId": "Tester01",
  "password": "Password123!",
  "passwordConfirm": "Password123!",
  "email": "test@example.com",
  "nickname": "tester"
}
```

### 정책

- `loginId`: 4~12자, 영어 대소문자/숫자만 허용
- `password`: 8~16자, 공백 금지, 영문/숫자/일반 특수문자만 허용
- `email`: 중복 제한, 로그인에는 사용하지 않음
- `nickname`: 2~12자, 중복 제한
- reserved nickname: `관리자`, `운영자`, `ADMIN`, `admin` 계열 금지

## POST /api/auth/login

로그인 API다.

### Request

```json
{
  "loginId": "Tester01",
  "password": "Password123!"
}
```

email 기반 로그인은 Phase 9 이후 사용하지 않는다.

## GET /api/auth/check-login-id

아이디 중복 확인 API다.

### Example

```http
GET /api/auth/check-login-id?loginId=Tester01
```

### Response

사용 가능:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "사용 가능한 아이디입니다.",
  "data": {
    "available": true
  }
}
```

중복:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "이미 존재하는 아이디입니다.",
  "data": {
    "available": false
  }
}
```

형식 오류:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "아이디는 4~12자의 영문 대소문자와 숫자만 사용할 수 있습니다.",
  "data": {
    "available": false
  }
}
```

이 API는 회원가입 화면에서 사용자의 입력 피드백을 빠르게 제공하기 위한 UX 보조 기능이다. 동시 가입 요청이나 우회 요청을 막기 위해 최종 검증은 회원가입 서버 로직과 `users.login_id` unique constraint에서 수행해야 한다. 사용자 enumeration 완화를 위한 rate limit/CAPTCHA는 이번 Phase에서 제외하며, 운영 고도화 단계에서 검토한다.

## GET /api/auth/check-nickname

닉네임 중복 확인 API다.

### Example

```http
GET /api/auth/check-nickname?nickname=tester
```

### Response

사용 가능:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "사용 가능한 닉네임입니다.",
  "data": {
    "available": true
  }
}
```

중복:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "이미 존재하는 닉네임입니다.",
  "data": {
    "available": false
  }
}
```

금칙어:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "사용할 수 없는 닉네임입니다.",
  "data": {
    "available": false
  }
}
```

길이 오류:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "닉네임은 2~12자여야 합니다.",
  "data": {
    "available": false
  }
}
```

이 API는 회원가입 화면에서 사용자의 입력 피드백을 빠르게 제공하기 위한 UX 보조 기능이다. 동시 가입 요청이나 우회 요청을 막기 위해 최종 검증은 회원가입 서버 로직과 nickname unique constraint에서 수행해야 한다. 사용자 enumeration 완화를 위한 rate limit/CAPTCHA는 이번 Phase에서 제외하며, 운영 고도화 단계에서 검토한다.
