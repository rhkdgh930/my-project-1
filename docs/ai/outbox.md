# Outbox Domain AI Rules

이 문서는 Outbox event 저장, claim, retry, recovery, duplicate publish 정책을 정의한다.

## 현재 정책

### 목적

- Outbox는 DB transaction 이후 실행되어야 하는 side effect를 안정적으로 처리한다.
- business transaction 안에서 외부 side effect를 직접 실행하지 않는다.
- handler는 idempotent해야 한다.
- payload에는 JPA Entity 자체를 저장하지 않는다.
- payload에는 raw token, password reset token, verification code 같은 secret을 저장하지 않는다.

### 상태 머신

- 상태는 `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `DEAD`를 사용한다.
- create 시 `PENDING`, `retryCount=0`, `nextRetryAt=now`다.
- 처리 대상은 `PENDING` 또는 `FAILED`이며 `nextRetryAt <= now`인 event다.
- worker는 처리 전 반드시 claim한다.
- claim 실패 시 handler를 실행하지 않는다.
- handler 성공 시 `SUCCESS`로 전환한다.
- handler 실패 시 `markFail(exception, now)`를 호출한다.
- retry count가 max를 넘으면 `DEAD`가 된다.
- handler 없음 또는 invalid payload 같은 non-retryable failure는 `DEAD` 처리를 고려한다.

### Retry / Backoff

- `OutboxEvent`는 `retryCount`, `lastTriedAt`, `nextRetryAt`, `lastError`를 관리한다.
- `markFail`은 retry count를 증가시킨다.
- `markFail`은 exponential backoff + jitter를 적용한다.
- backoff는 최대 60초 제한을 둔다.
- `lastError`는 1000자까지 저장한다.

### Processing Timeout Recovery

- `PROCESSING` 상태로 오래 멈춘 event는 recovery 대상이다.
- recovery는 threshold 이전 `PROCESSING` event를 찾아 `markProcessingTimeout(now)`을 호출한다.
- timeout recovery도 retry count를 증가시킨다.
- max retry 초과 시 `DEAD`가 된다.
- recovery scheduler는 stuck event 복구를 주기적으로 시도한다.

### Admin Retry

- `resetForRetry(now)`는 status를 `PENDING`으로 되돌린다.
- `retryCount=0`, `lastError=null`, `lastTriedAt=null`, `nextRetryAt=now`로 초기화한다.
- admin retry는 retry 가능한 상태에서만 허용한다.

### Cleanup

- `deleteSuccessBefore(threshold)`는 성공 event cleanup 용도다.
- 성공 event 보존 기간은 운영 정책으로 관리한다.

### Event Key

- `eventKey`는 idempotency key다.
- 같은 logical event가 중복되면 안 되는 경우 deterministic key를 사용한다.
- 매번 별도 처리가 필요한 event는 UUID 기반 key를 사용할 수 있다.
- eventKey 정책 변경은 중복 처리 의미 변경이므로 신중히 한다.

### publish / publishIfAbsent

- 일반 `publish(type, payload, eventKey)`는 event를 저장하고 `OutboxSavedEvent`를 발행한다.
- 일반 `publish`는 중복 `eventKey`를 오류로 드러내는 정책을 유지한다.
- `publishIfAbsent(type, payload, eventKey)`는 중복 발행이 정상적으로 발생할 수 있는 이벤트에만 사용한다.
- 현재 `DORMANCY_NOTIFY` 발행에서 사용한다.
- `eventKey`가 이미 있으면 `false`를 반환하고 event를 저장하지 않는다.
- 기존 event가 있으면 `OutboxSavedEvent`도 발행하지 않는다.
- 저장 전 `existsByEventKey(eventKey)`로 사전 체크한다.
- 사전 체크와 저장 사이 race는 DB unique constraint로 방어한다.
- `saveAndFlush` 중 `DataIntegrityViolationException`이 발생하면 중복으로 보고 `false`를 반환한다.
- 저장 성공 시에만 `true`를 반환하고 `OutboxSavedEvent`를 발행한다.

### 현재 주요 Event

- `POST_CREATED`: post 생성 후 image attach
- `POST_UPDATED`: post 수정 후 image sync
- `POST_DELETED`: post 삭제 후 image detach
- `USER_ACCOUNT_CHANGED`: user 상태 변경 후 auth cache/token side effect
- `DORMANCY_NOTIFY`: 휴면 알림

## 테스트 기준

- repository는 processable id 조회, claim 성공/실패, stuck processing 조회, success cleanup을 검증한다.
- processor는 claim 실패 시 handler 미실행, handler 성공/실패 상태 전이를 검증한다.
- recovery는 stuck PROCESSING event의 FAILED/DEAD 전이를 검증한다.
- event test는 retry/backoff/jitter를 exact second가 아니라 범위로 검증한다.
- admin retry는 `resetForRetry` 정책과 retry 불가 상태를 검증한다.
- `OutboxPublisherTest`는 `publish`와 `publishIfAbsent`의 저장/발행/중복 race 정책을 검증한다.

## TODO / 운영 전 개선

- email 발송 exactly-once는 현재 보장하지 않는다.
- email handler는 중복 발송 가능성을 줄이기 위한 dedupe key 또는 발송 이력 정책을 검토한다.
- Outbox SUCCESS event retention 기간을 운영 정책으로 명확히 한다.
- event payload versioning이 필요해지면 별도 버전 필드를 검토한다.
- handler별 retryable/non-retryable exception 분류를 더 명확히 할 수 있다.
