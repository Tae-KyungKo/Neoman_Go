# Troubleshooting

## KI-094. SSE text/event-stream 응답에 JSON ErrorResponse를 쓰려는 문제

### 현상

- 운영 로그에 `HttpMessageNotWritableException` 발생
- `No converter for ErrorResponse with preset Content-Type 'text/event-stream'`

### 추정 원인

- SSE 응답 Content-Type이 `text/event-stream`으로 잡힌 상태에서 `GlobalExceptionHandler`가 JSON ErrorResponse를 반환하려 함

### 영향

- health check 및 핵심 기능에는 현재 영향 없음
- 로그에 불필요한 stack trace가 남음
- SSE 인증 실패/연결 실패 케이스의 응답 처리가 명확하지 않음

### 처리 원칙

- SSE 연결 전 인증 실패는 401로 종료
- SSE 연결 후 예외는 emitter cleanup
- `GlobalExceptionHandler`는 SSE 응답에 JSON ErrorResponse를 쓰지 않음
- 클라이언트 종료, broken pipe 계열은 WARN stack trace를 남기지 않음
- SSE 전송 실패 시 Notification DB 저장은 유지하고 emitter만 제거
