# Common AI Rules

이 문서는 공통 예외 응답, serialization, time, logging, config 정책을 정리한다.

## 현재 정책 - Exception Response

- 비즈니스 예외는 `CustomException(ErrorCode.X)`를 사용한다.
- 일반 예외 응답은 `ExceptionResponse` shape를 유지한다.
- validation error 응답은 field별 errors를 제공하는 `ValidExceptionResponse` shape를 유지한다.
- Security filter/handler 영역의 JSON error response는 `ErrorResponseWriter`가 작성한다.
- `ErrorCode`의 status/code/message는 API contract에 가깝다.
- `ErrorCode` status 변경은 API contract 변경으로 본다.

## 현재 정책 - Serialization / Time

- `DataSerializer`는 JSON 직렬화/역직렬화와 Java Time 호환을 담당한다.
- Outbox payload와 Redis JSON payload는 `DataSerializer` 변경 영향을 받는다.
- Admin Audit Log의 `metadata`도 `DataSerializer` 기반 JSON 문자열로 저장한다.
- 시간 의존 로직은 가능한 `Clock` bean을 사용한다.
- JSON 응답과 문서는 UTF-8 인코딩을 유지한다.

## 현재 정책 - Logging / MDC

- password, raw token, refresh token, verification code, password reset token을 로그에 남기지 않는다.
- request/response logging은 유지한다.
- trace id는 MDC로 관리한다.
- 인증된 사용자라면 userId를 MDC에 추가할 수 있다.
- async 작업은 MDC context propagation을 유지한다.
- servlet filter 자동 등록을 직접 제어하는 테스트가 있으므로 중복 등록에 주의한다.

## 현재 정책 - Config Boundary

- Common은 특정 도메인 비즈니스 규칙을 직접 알지 않는다.
- Password encoding, clock, async, mail, swagger, web resource config는 공통 인프라로 관리할 수 있다.
- 도메인 세부 정책은 각 도메인 문서에 둔다.

## 주의사항

- 기존 응답 field는 삭제하지 않는다.
- transition field는 제거 일정을 명확히 정하기 전까지 유지한다.
- additive field 추가는 하위 호환 변경으로 취급하되 frontend null-safe 렌더링을 확인한다.

## TODO

- multipart size 초과 예외 응답을 명시적으로 보강한다.
- structured logging 또는 trace id response header가 필요하면 common 정책으로 추가한다.
- ErrorCode 분류 체계가 커지면 domain grouping을 검토한다.
