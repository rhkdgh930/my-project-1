# Testing AI Rules

## 목적

이 문서는 Codex가 변경한 코드를 검증하는 기준을 정의한다.

테스트는 현재 구조를 고정하기 위한 것이 아니라, 안전하게 발전시키기 위한 장치다.

---

## 기본 원칙

- 버그 수정은 재현 테스트를 우선한다.
- 리팩토링은 동작 보존 테스트를 우선한다.
- 구조 개선은 단계별 검증을 우선한다.
- 테스트를 실행하지 못했으면 실행하지 못했다고 말한다.
- 빌드를 실행하지 않았으면 빌드 성공이라고 말하지 않는다.
- 테스트 DisplayName은 한글로 적어야 한다.

---

## 테스트 우선순위

1. Domain unit test
2. Application service test
3. Outbox processor/handler test
4. Security/JWT/filter test
5. Repository query test
6. Controller request/response test
7. End-to-end test

---

## Surgical Mode 테스트 기준

작은 변경에서는 다음을 우선한다.

- 변경한 메서드 주변 테스트
- 기존 동작 보존 테스트
- regression test
- 최소한의 build/test command

---

## Architecture Review Mode 테스트 기준

구조 검토만 하는 경우 코드를 수정하지 않는다.

대신 다음을 제안한다.

- 현재 테스트 공백
- 가장 먼저 추가해야 할 테스트
- 리팩토링 전 안전망 테스트
- 리팩토링 후 검증 테스트

---

## Evolutionary Refactor Mode 테스트 기준

구조를 점진적으로 개선하는 경우 다음 순서를 따른다.

1. 현재 동작을 고정하는 characterization test 작성
2. 작은 리팩토링 적용
3. 테스트 통과 확인
4. 다음 단계 진행

---

## Domain Test 기준

다음 변경은 domain test를 우선한다.

- User lifecycle 변경
- withdrawal 정책 변경
- suspension 정책 변경
- dormancy 정책 변경
- Email validation 변경
- ProfileDetail validation 변경
- 상태 전이 method 변경

검증 예시:

- ACTIVE user가 탈퇴 요청 가능
- WITHDRAWN_REQUESTED user가 복구 가능 기간 내 복구 가능
- 복구 가능 기간이 지나면 복구 불가
- 탈퇴 완료 시 개인정보 masking
- 휴면 유저 로그인 시 ACTIVE 전환
- 차단 유저는 full access 불가
- permanent suspension은 만료되지 않음

---

## Service Test 기준

다음 변경은 service test를 고려한다.

- UserCommandService
- AuthService
- UserLoginService
- AdminUserCommandService
- Batch worker
- Redis service orchestration
- Outbox publishing

검증 예시:

- 비밀번호 변경 시 현재 비밀번호 검증
- 비밀번호 변경 후 token invalidation event 발행
- 탈퇴 요청 후 cache/token invalidation event 발행
- 로그인 성공 후 lastLoginAt 갱신
- 휴면 해제 시 Outbox event 발행
- password reset token 검증 후 password 변경

---

## Outbox Test 기준

다음 변경은 Outbox test를 고려한다.

- OutboxProcessor
- OutboxEventManager
- OutboxRepository claim query
- Handler
- Retry/dead-letter policy
- Recovery scheduler
- Admin retry

검증 예시:

- claim 실패 시 handler 실행 안 됨
- handler 성공 시 SUCCESS
- handler 실패 시 FAILED
- retryCount 초과 시 DEAD
- handler 없음이면 DEAD
- invalid payload 처리
- PROCESSING stuck event가 FAILED로 복구됨
- 상태 업데이트가 handler transaction 실패에 휘말리지 않음

---

## Security Test 기준

다음 변경은 Security/Auth test를 고려한다.

- JwtProvider
- JwtAuthenticationFilter
- JwtLoginFilter
- JwtLoginSuccessHandler
- JwtLoginFailureHandler
- AuthService reissue/logout
- RedisTokenService
- RedisUserContextService

검증 예시:

- expired access token 응답
- expired refresh token 응답
- refresh token type 검증
- access token type 검증
- blacklist token 거부
- refresh token hash mismatch 시 기존 refresh token 삭제
- suspended user 인증 거부
- withdrawn requested user 인증 거부
- dormant user 인증 거부
- login failure count 증가

---

## Repository Test 기준

다음 변경은 repository test를 고려한다.

- cursor query
- dormant user query
- withdrawal cleanup query
- outbox processable query
- outbox claim query
- cleanup query

---

## MUST

- 변경한 behavior에 대한 검증 방법을 설명한다.
- 테스트를 추가하지 않는다면 이유를 설명한다.
- 테스트를 실행하지 못했다면 명확히 말한다.
- 실패하는 테스트를 무시하지 않는다.
- 테스트를 통과시키기 위해 assertion을 약화하지 않는다.
- 기존 테스트를 삭제할 경우 이유를 설명한다.
- 테스트 DisplayName은 한글로 적어야 한다. 

---

## SHOULD

- 작은 단위 테스트를 우선한다.
- 외부 side effect는 mock/fake 처리한다.
- Redis가 필요한 테스트는 test container 또는 명확한 대안을 사용한다.
- transaction boundary가 중요한 테스트는 단순 mock만으로 충분하다고 주장하지 않는다.
- 테스트 이름은 behavior 중심으로 작성한다.

---

## DO NOT

- 실행하지 않은 테스트를 실행했다고 말하지 않는다.
- 빌드하지 않았는데 빌드 성공이라고 말하지 않는다.
- unrelated test를 수정하지 않는다.
- flaky test를 이유 없이 비활성화하지 않는다.
- 테스트 없이 Outbox transaction 구조를 크게 바꾸지 않는다.
- 테스트 없이 SecurityConfig를 크게 바꾸지 않는다.