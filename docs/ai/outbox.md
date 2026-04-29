# Outbox Domain AI Rules

## 목적

Outbox 도메인은 DB transaction 이후 실행되어야 하는 side effect를 안정적으로 처리한다.

현재 주요 책임은 다음과 같다.

- Outbox event 저장
- Event key 기반 중복 방지
- After commit async processing
- Scheduler fallback processing
- Claim 기반 동시성 제어
- Retry/backoff
- Dead-letter 처리
- Stuck PROCESSING recovery
- SUCCESS event cleanup
- Event type별 handler dispatch

이 문서는 Outbox를 무조건 모든 곳에 강제하기 위한 문서가 아니다.

목적은 side effect의 신뢰성과 transaction boundary를 보호하면서, 필요한 경우 더 적절한 이벤트/메시징 구조를 제안할 수 있게 하는 것이다.

---

## 현재 구조의 기본 의도

현재 Outbox 흐름:

1. Business transaction 안에서 Outbox event 저장
2. Transaction commit 이후 listener가 async processing 시도
3. Scheduler가 fallback으로 미처리 event 처리
4. Processor가 event를 claim
5. Handler가 side effect 수행
6. 성공 시 SUCCESS
7. 실패 시 FAILED 또는 DEAD
8. stuck PROCESSING event는 recovery scheduler가 복구

---

## HARD RULES

다음은 반드시 지킨다.

- 중요한 side effect를 business transaction 내부에서 직접 실행하지 않는다.
- Event를 처리하기 전 반드시 claim한다.
- Claim 실패 시 handler를 실행하지 않는다.
- Handler 실패를 SUCCESS로 기록하지 않는다.
- 실패한 event의 retry/dead 처리를 제거하지 않는다.
- 장애 복구용 scheduler fallback을 단순화를 이유로 제거하지 않는다.
- stuck PROCESSING recovery를 제거하지 않는다.
- handler transaction 실패 때문에 outbox 상태 기록이 유실되지 않도록 한다.
- Payload에 JPA Entity 자체를 저장하지 않는다.
- 외부 side effect를 domain entity에서 직접 호출하지 않는다.
- event status를 여러 곳에서 무질서하게 변경하지 않는다.

---

## DEFAULT RULES

작은 변경에서는 현재 구조를 유지한다.

- OutboxEvent는 persistent table에 저장한다.
- Event status는 `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `DEAD`를 사용한다.
- `PENDING`, `FAILED` 상태만 process 대상이다.
- `PROCESSING`은 worker가 점유한 상태다.
- `OutboxEventManager`가 상태 전이를 담당한다.
- Processor는 handler dispatch를 담당한다.
- Handler는 하나의 event type을 담당한다.
- Retry는 backoff + jitter 구조를 사용한다.
- 최대 retry 초과 시 DEAD 처리한다.
- SUCCESS event cleanup을 유지한다.
- eventKey uniqueness를 유지한다.

---

## IMPROVEMENT OPTIONS

다음 개선은 제안 가능하다.

단, 바로 적용하지 말고 tradeoff를 먼저 설명한다.

- 특정 side effect가 Outbox에 적합한지 재검토
- event key/idempotency 정책 개선
- handler idempotency 강화
- retry policy 설정화
- dead-letter reason 구조화
- payload versioning 추가
- event type별 payload validation 강화
- Outbox table indexing 개선
- batch processing 성능 개선
- message broker 도입 가능성 검토
- Outbox와 domain event 역할 분리
- handler 내부 transaction 정책 개선
- admin retry 정책 개선

---

## Outbox를 유지해야 하는 경우

다음 side effect는 기본적으로 Outbox가 적합하다.

- DB 상태 변경 이후 반드시 수행되어야 하는 작업
- 실패 시 retry가 필요한 작업
- 외부 시스템 호출
- email 발송
- image attach/sync
- auth cache/token invalidation
- batch lifecycle 후속 처리
- process crash 이후에도 복구되어야 하는 작업

---

## Outbox가 과할 수 있는 경우

다음 경우에는 대안을 제안할 수 있다.

- 실패해도 복구할 필요가 없는 단순 log성 작업
- 같은 transaction 안에서 반드시 즉시 끝나야 하는 작업
- retry가 오히려 위험한 작업
- idempotency를 보장할 수 없는 외부 호출
- 사용자에게 즉시 결과를 알려야 하는 작업

대안 제안 시 비교한다.

- direct call
- application event
- async event
- Outbox
- message broker

비교 기준:

- reliability
- transaction boundary
- retry 가능성
- idempotency
- user experience
- operational complexity

---

## Processor 기준

Processor는 기본적으로 다음 흐름을 유지한다.

1. claim
2. snapshot 없으면 return
3. handler 조회
4. handler 없으면 DEAD
5. handler 실행
6. 성공 시 markSuccess
7. 실패 시 markFail
8. error log 기록

개선 가능:

- handler 없음과 payload invalid를 DEAD로 분리
- retryable exception과 non-retryable exception 분리
- failure reason 구조화
- logging context 강화

---

## Handler 기준

Handler 기본 규칙:

- `OutboxHandler`를 구현한다.
- 하나의 handler는 하나의 event type을 담당한다.
- Payload를 deserialize한다.
- Payload를 검증한다.
- 하나의 side effect concern만 수행한다.
- 실패 시 예외를 던진다.
- 직접 outbox status를 변경하지 않는다.

개선 가능:

- payload validator 추가
- idempotency key 사용
- 이미 처리된 외부 side effect 감지
- handler별 timeout/retry 정책 분리

---

## Event Key 기준

`eventKey`는 idempotency 정책이다.

- 같은 logical event가 중복되면 안 되는 경우 deterministic key를 사용한다.
- 매번 새 event가 필요하면 UUID를 사용할 수 있다.
- eventKey 변경은 중복 처리 정책 변경이므로 신중하게 한다.
- eventKey는 중복 발행 방지용이다.
  중복 발생 시 무시할지 실패시킬지는 이벤트 타입 정책에 따라 결정한다.
  무조건 catch해서 무시하지 않는다.

개선 제안 가능:

- event별 key 생성 정책 문서화
- deterministic key와 random key 사용 기준 분리
- unique constraint 충돌 시 처리 정책 추가

---

## Outbox 변경 시 검증 기준

Outbox 변경 시 다음 테스트를 고려한다.

- claim 성공
- claim 실패 시 handler 미실행
- handler 성공 시 SUCCESS
- handler 실패 시 FAILED
- retryCount 초과 시 DEAD
- handler 없음 시 DEAD
- invalid payload 처리
- stuck PROCESSING recovery
- SUCCESS cleanup
- admin retry
- eventKey duplicate 처리
- transaction rollback 상황에서 fail state 유실 여부

---

## 명시적 요청 없이는 하지 말 것

- Persistent Outbox 제거
- 단순 `@Async` 호출로 대체
- claim 구조 제거
- retry/dead/recovery 제거
- Outbox 상태 전이를 handler에 분산
- business transaction 안에서 handler 직접 실행
- scheduler fallback 제거

---

## Retry 가능 여부 기준

모든 실패가 retry 대상은 아니다.

Codex는 handler 실패를 다룰 때 가능하면 다음을 구분해야 한다.

- Retryable failure: 일시적 네트워크 오류, 외부 서비스 장애, Redis 일시 장애
- Non-retryable failure: payload invalid, handler not found, 잘못된 event type, 필수 데이터 누락

Non-retryable failure는 무한 retry하지 말고 DEAD 처리를 검토한다.

## Secret Payload Policy

Outbox payload에는 raw token, password reset token, verification code, one-time code,
session credential, authorization credential을 저장하지 않는다.

짧은 TTL을 가진 인증성 secret은 Outbox durable retry 대상보다 secret minimization을 우선한다.

다음 조건을 모두 만족하면 Outbox 사용을 재검토한다.

- payload에 secret 또는 bearer credential이 포함된다.
- secret TTL이 짧다.
- 사용자가 재요청할 수 있다.
- retry 지연 후 실행되면 보안 또는 UX 의미가 약해진다.

Password reset link와 email verification code는 기본적으로 persistent Outbox payload에 저장하지 않는다.
필요하면 synchronous 처리, encrypted payload, reference-based secret store 중 하나를 명시적으로 선택한다.

Outbox에 남기는 side effect는 다음 조건을 만족해야 한다.

- retry가 의미 있다.
- handler가 idempotent하다.
- payload가 장기 보관되어도 보안상 허용 가능하다.
- 늦게 처리되어도 비즈니스 의미가 유지된다.