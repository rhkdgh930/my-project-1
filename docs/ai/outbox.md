# Outbox AI Rules

이 문서는 Outbox pattern, claim 기반 처리, retry/recovery/admin retry 정책을 정리한다.

## 현재 정책 - 기본 구조

- 도메인 트랜잭션 안에서는 Outbox event를 저장한다.
- 실제 side effect는 Outbox processor/handler가 처리한다.
- event type, eventKey, payload shape는 테스트로 보호한다.
- handler는 재시도될 수 있으므로 idempotency를 전제로 설계한다.

## 현재 정책 - 상태 머신

- 주요 상태는 `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `DEAD`이다.
- processor는 `claim()` 성공한 이벤트만 handler를 실행한다.
- claim 실패 시 handler를 호출하지 않는다.
- handler가 없으면 `DEAD`로 기록한다.
- handler 성공 시 `SUCCESS`로 기록한다.
- handler 실패 시 retry 가능 여부와 retry count에 따라 `FAILED` 또는 `DEAD`로 기록한다.
- `markFail`은 `retryCount`, `nextRetryAt`, `lastError`를 갱신한다.
- retry는 backoff와 jitter를 사용하며 exact second에 의존하지 않는다.

## 현재 정책 - Recovery

- stuck `PROCESSING` 이벤트는 recovery가 `FAILED` 또는 `DEAD`로 복구한다.
- max retry에 도달한 stuck `PROCESSING` 이벤트는 `DEAD`로 복구한다.
- 현재 시각 기준 일정 시간 이전 `PROCESSING` 이벤트만 stuck 대상으로 본다.
- 조회 후 이미 `SUCCESS`가 된 이벤트는 되돌리지 않는다.

## 현재 정책 - Retry / Admin

- `resetForRetry`는 재처리 가능한 이벤트를 `PENDING`으로 되돌린다.
- admin retry와 retry-now는 `FAILED` 또는 `DEAD` 상태를 재처리 대상으로 삼는다.
- `SUCCESS`는 already succeeded 정책으로 재처리를 거부한다.
- `PENDING`은 already pending 정책으로 재처리를 거부한다.
- `PROCESSING`은 already processing 또는 409 계열 정책으로 재처리를 거부한다.
- 상태별 retry 불가 ErrorCode는 API contract로 취급한다.

## 현재 정책 - 중복 방어 / Cleanup

- 일반 `publish`와 `publishIfAbsent`는 구분한다.
- `publishIfAbsent`는 중복 가능 이벤트에만 사용한다.
- 현재 `DORMANCY_NOTIFY` 중복 방어에 `publishIfAbsent`를 사용한다.
- `deleteSuccessBefore`는 성공 이벤트 cleanup 용도다.
- SUCCESS event retention 기간은 운영 정책으로 정한다.

## 현재 정책 - 주요 Handler

- `USER_ACCOUNT_CHANGED`는 사용자 컨텍스트 캐시 evict와 refresh token 삭제를 처리한다.
- `DORMANCY_NOTIFY`는 휴면 알림 메일 side effect를 처리한다.
- `POST_CREATED`, `POST_UPDATED`, `POST_DELETED`는 이미지 sync를 처리한다.
- payload 필수 값이 null이면 side effect를 실행하지 않거나 현재 handler 정책대로 실패 처리한다.

## 주의사항

- 현재 없는 event type을 문서에 현재 정책처럼 추가하지 않는다.
- handler payload를 길게 복붙하지 말고 type, key, side effect 중심으로 관리한다.

## TODO

- email exactly-once는 현재 미보장이다.
- email 중복 발송 완화가 필요하면 dedupe key 또는 발송 이력 정책을 검토한다.
- handler별 retryable/non-retryable exception 분류를 강화할 수 있다.
- SUCCESS event retention 기간과 cleanup scheduler 주기를 운영 정책으로 확정한다.
