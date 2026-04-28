# Project AI Rules

## 목적

이 프로젝트는 Spring Boot 기반 백엔드이며, 현재 다음 방향성을 가지고 있다.

- 실용적인 DDD 스타일
- Command / Query 성격의 서비스 분리
- Spring Security + JWT 기반 인증
- Redis 기반 인증 상태 관리
- Outbox 기반 side effect 처리
- User aggregate 중심의 계정 lifecycle 관리

이 문서의 목적은 현재 구조를 무조건 고정하는 것이 아니다.

목적은 다음과 같다.

1. 보안, 데이터 정합성, 장애 복구에 중요한 규칙은 강하게 보호한다.
2. 현재 구조를 기본값으로 삼는다.
3. 더 나은 구조가 가능하면 AI가 제안할 수 있게 한다.
4. 단, 큰 구조 변경은 바로 적용하지 않고 tradeoff와 migration plan을 먼저 설명한다.

---

## 규칙 강도

이 프로젝트의 AI 규칙은 세 단계로 해석한다.

### HARD RULE

보안, 데이터 정합성, 장애 복구, API 계약, 민감정보 보호와 관련된 규칙이다.

명시적 요청 없이는 깨면 안 된다.

예:

- raw token/password/verification code를 로그에 남기지 않는다.
- 인증 실패 시 SecurityContext를 정리한다.
- 실패한 side effect를 성공 처리하지 않는다.
- 테스트를 실행하지 않았으면 실행했다고 말하지 않는다.

### DEFAULT RULE

현재 프로젝트의 기본 구조다.

작은 버그 수정, 기능 추가, 제한된 리팩토링에서는 이 구조를 유지한다.

예:

- User aggregate 중심 lifecycle 관리
- Filter 기반 JWT 로그인
- Redis 기반 refresh token 관리
- Outbox 기반 side effect 처리
- ErrorCode 기반 예외 응답

### IMPROVEMENT OPTION

현재 구조보다 더 나은 구조가 가능하면 제안할 수 있다.

단, 바로 적용하지 말고 먼저 다음을 설명해야 한다.

1. 현재 구조의 문제점
2. 제안하는 대안
3. 장점
4. 단점
5. migration plan
6. 작은 단계로 나눠 적용하는 방법
7. 검증 방법

---

## 전체 아키텍처 기본값

현재 프로젝트는 다음 구조를 기본값으로 한다.

- Controller는 얇게 유지한다.
- Application Service는 orchestration을 담당한다.
- Domain Entity / Value Object는 핵심 비즈니스 규칙을 가진다.
- Repository는 DB 접근을 담당한다.
- Redis는 임시 상태 저장소/cache/token store로 사용한다.
- DB는 user/account lifecycle의 source of truth이다.
- Outbox는 중요한 side effect의 신뢰성 있는 처리에 사용한다.
- Common은 공통 인프라만 담당한다.

---

## HARD RULES

다음은 반드시 지킨다.

- Controller에 핵심 비즈니스 규칙을 넣지 않는다.
- Entity에서 Repository, Redis, 외부 API, MailSender를 직접 호출하지 않는다.
- Password, raw token, verification code, password reset token을 로그에 남기지 않는다.
- 테스트나 빌드를 실행하지 않았으면 실행했다고 말하지 않는다.
- API response shape을 임의로 변경하지 않는다.
- 보안 관련 실패를 조용히 무시하지 않는다.
- transaction rollback 가능성이 있는 코드를 숨기지 않는다.
- 장애 복구 흐름을 단순화를 이유로 제거하지 않는다.
- 요청 범위를 넘어선 대규모 formatting/rewrite를 하지 않는다.

---

## DEFAULT RULES

작은 작업에서는 다음 구조를 유지한다.

- User 상태 변경은 domain method를 우선 사용한다.
- Auth는 Spring Security Filter 기반 구조를 유지한다.
- Refresh Token은 Redis에 hash로 저장한다.
- Access Token logout은 blacklist 방식으로 처리한다.
- User context cache는 Redis를 사용할 수 있지만 DB가 최종 기준이다.
- 중요한 side effect는 Outbox를 통해 처리한다.
- ErrorCode + CustomException 기반 예외 처리를 유지한다.
- 시간 의존 로직은 Clock을 사용한다.
- 변경은 surgical change를 우선한다.

---

## IMPROVEMENT OPTIONS

다음은 제안 가능하지만, 바로 적용하지 말고 먼저 설명해야 한다.

- User aggregate가 과도하게 커질 경우 정책 객체나 domain service로 일부 분리
- Auth와 User 사이 의존 방향 개선
- UserDetailsImpl 책임 축소
- Redis user context cache 전략 개선
- Outbox event key/idempotency 정책 개선
- Email verification/password reset flow의 consistency 개선
- OAuth2 redirect/cookie/security 정책 개선
- Batch transaction 단위 개선
- ErrorCode 분류 체계 개선
- 테스트 구조 개선

---

## 작업 모드

Codex에게 요청할 때는 아래 모드 중 하나를 명시하는 것을 권장한다.

### Surgical Mode

작은 수정, 버그 수정, 제한된 리팩토링에 사용한다.

규칙:

- 현재 구조를 유지한다.
- 가장 작은 안전한 변경만 수행한다.
- public API contract를 변경하지 않는다.
- 관련 없는 코드를 정리하지 않는다.

### Architecture Review Mode

현재 구조 자체를 검토할 때 사용한다.

규칙:

- 코드를 수정하지 않는다.
- 현재 구조를 비판적으로 검토한다.
- 문제를 중요도별로 나눈다.
- 개선안을 small / medium / major로 분류한다.
- tradeoff와 migration plan을 제시한다.

### Evolutionary Refactor Mode

구조를 유지하면서 점진적으로 개선할 때 사용한다.

규칙:

- 현재 구조의 의도를 유지한다.
- 더 나은 구조가 있으면 제안한다.
- 큰 변경은 작은 단계로 나눈다.
- 각 단계마다 검증 방법을 제시한다.

---

## 작업 전 답변 규칙

구현 전에 Codex는 다음을 먼저 답해야 한다.

1. Assumptions
2. Current design intent
3. Architecture invariants to preserve
4. Possible better alternatives, if any
5. Chosen approach
6. Files likely to change
7. Verification plan

---

## 작업 후 답변 규칙

구현 후 Codex는 다음을 답해야 한다.

1. Summary of changes
2. Why the architecture is preserved or improved
3. Behavior changes, if any
4. Tests/build commands run
5. Anything not verified
6. Follow-up recommendations, if any