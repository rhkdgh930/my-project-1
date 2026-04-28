# Common Domain AI Rules

## 목적

Common 패키지는 여러 도메인이 공유하는 인프라 성격의 코드를 담당한다.

현재 주요 책임은 다음과 같다.

- 공통 예외
- ErrorCode
- Exception response
- Validation response
- Logging/MDC
- Async executor
- Clock config
- Mail config
- Swagger config
- Web resource config
- Data serialization
- Page response
- Base entity

이 문서는 Common 구조를 무조건 고정하기 위한 문서가 아니다.

목적은 공통 계약을 안정적으로 유지하면서, 필요하면 더 나은 공통 인프라 구조로 발전시키는 것이다.

---

## HARD RULES

다음은 반드시 지킨다.

- 민감 정보를 로그에 남기지 않는다.
    - password
    - raw token
    - refresh token
    - verification code
    - password reset token
- 공통 패키지가 특정 도메인 비즈니스 로직에 의존하지 않게 한다.
- Error response shape을 임의로 변경하지 않는다.
- 알 수 없는 예외를 조용히 삼키지 않는다.
- 테스트 없이 serialization 방식을 크게 바꾸지 않는다.
- 시간 의존 로직에서 `Clock` 사용 가능성을 무시하지 않는다.
- 공통 유틸을 이유 없이 비대하게 만들지 않는다.

---

## DEFAULT RULES

작은 변경에서는 현재 구조를 유지한다.

- 비즈니스 예외는 `CustomException(ErrorCode.X)`를 사용한다.
- Validation error는 field별 errors를 제공한다.
- GlobalExceptionHandler가 공통 예외 응답을 담당한다.
- HTTP request/response logging을 유지한다.
- traceId는 MDC로 관리한다.
- 인증된 사용자의 userId는 MDC에 추가한다.
- Async 작업에는 MDC context를 전달한다.
- Batch 작업은 BatchTraceHelper를 사용한다.
- `DataSerializer`는 Java Time과 호환된다.
- `Clock` bean을 사용한다.
- `PageResponse` 구조를 안정적으로 유지한다.

---

## IMPROVEMENT OPTIONS

다음 개선은 제안 가능하다.

단, 바로 적용하지 말고 tradeoff를 먼저 설명한다.

- ErrorCode 분류 체계 개선
- security exception response와 common exception response 일관성 개선
- logging prefix 표준화
- structured logging 개선
- traceId response header 추가
- DataSerializer 책임 축소 또는 ObjectMapper bean 활용
- validation response 개선
- BaseEntity soft delete 정책 재검토
- MailConfig 설정 외부화
- Async executor 설정 개선
- PageResponse 확장

---

## ErrorCode 기준

ErrorCode는 API contract에 가깝다.

- 이름은 안정적으로 유지한다.
- message 변경은 사용자/API 영향이 있을 수 있다.
- status 변경은 API contract 변경이다.
- 새로운 비즈니스 실패가 생기면 ErrorCode 추가를 우선 검토한다.

개선 제안 가능:

- Auth/User/Outbox/Common별 ErrorCode grouping
- status code 재검토
- 중복 message 정리
- client-facing message와 internal reason 분리

---

## Exception 기준

기본 정책:

- 예상 가능한 비즈니스 예외: `CustomException`
- validation 예외: `ValidExceptionResponse`
- security 예외: Auth handler와 일관성 유지
- 예상 못한 예외: `INTERNAL_SERVER_ERROR`

개선 제안 가능:

- exception response factory
- security/common response 통합
- error logging level 정리
- exception별 monitoring hook 추가

---

## Logging 기준

기본 정책:

- request 시작/종료 로그 유지
- traceId 유지
- userId MDC 유지
- async MDC propagation 유지
- batch trace 유지
- 민감 정보 로그 금지

개선 제안 가능:

- response header에 traceId 추가
- duration logging 개선
- error log format 표준화
- structured JSON logging 전환

---

## Serialization 기준

`DataSerializer`는 여러 곳에서 사용할 수 있으므로 변경에 주의한다.

기본 역할:

- Object to JSON
- JSON to Object
- InputStream to Object
- tryDeserialize

변경 시 고려:

- Outbox payload 호환성
- Redis에 저장된 JSON 호환성
- Java Time serialization
- unknown property 처리

---

## Common 변경 시 검증 기준

Common 변경 시 다음 테스트를 고려한다.

- ErrorCode response shape
- CustomException handling
- Validation error response
- DataSerializer Java Time serialization
- DataSerializer unknown property handling
- MDC propagation
- PasswordValidator
- PageResponse
- Clock injection usage

---

## 명시적 요청 없이는 하지 말 것

- Error response shape 변경
- Common에 domain-specific logic 추가
- MDC 전략 제거
- DataSerializer를 특정 도메인 전용으로 변경
- BaseEntity 삭제 정책 변경
- GlobalExceptionHandler 전체 rewrite