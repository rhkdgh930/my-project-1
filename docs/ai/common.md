# Common AI Rules

이 문서는 공통 예외 응답, logging, config, serialization 정책을 정의한다.

## 현재 정책

### Exception Response

- 비즈니스 예외는 `CustomException(ErrorCode.X)`를 사용한다.
- 공통 예외 응답은 `ExceptionResponse` shape를 유지한다.
- validation error는 field별 errors를 제공하는 `ValidExceptionResponse` shape를 유지한다.
- Security handler/filter 영역의 JSON error response는 `ErrorResponseWriter`가 작성한다.
- ErrorCode의 status/code/message는 API contract에 가깝다.
- ErrorCode status 변경은 API contract 변경으로 본다.

### Logging

- password, raw token, refresh token, verification code, password reset token을 로그에 남기지 않는다.
- request/response logging은 유지한다.
- traceId는 MDC로 관리한다.
- 인증된 사용자라면 userId를 MDC에 추가할 수 있다.
- async 작업은 MDC context propagation을 유지한다.
- batch 작업은 batch trace helper를 사용할 수 있다.

### Serialization / Time

- `DataSerializer`는 JSON 직렬화/역직렬화와 Java Time 호환을 담당한다.
- Outbox payload와 Redis JSON payload는 `DataSerializer` 변경 영향을 받는다.
- 시간 의존 로직은 가능한 `Clock` bean을 사용한다.

### Config

- Common은 특정 도메인 비즈니스 규칙을 직접 알지 않는다.
- Password encoding, clock, async, mail, swagger, web resource config는 공통 인프라로 관리할 수 있다.
- 도메인 세부 정책은 각 도메인 문서에 둔다.

## 테스트 기준

- `GlobalExceptionHandler`의 `CustomException` 응답 shape를 검증한다.
- validation error response shape를 검증한다.
- security error response는 Auth/Security 테스트에서 함께 검증한다.
- `DataSerializer` Java Time serialization과 unknown property 처리를 검증한다.
- MDC propagation이 필요한 async/batch 경로는 별도 테스트를 고려한다.

## TODO / 운영 전 개선

- multipart size 초과 예외 응답을 명시적으로 보강한다.
- structured logging 또는 traceId response header가 필요하면 common 정책으로 추가한다.
- ErrorCode 분류 체계가 커지면 domain grouping을 검토한다.
