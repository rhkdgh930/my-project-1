# Testing AI Rules

이 문서는 테스트 작성, 실행, 검증 보고 기준을 정리한다.

## 현재 정책 - 기본 원칙

- 변경한 behavior는 테스트로 검증한다.
- 테스트를 통과시키기 위해 production 정책을 약화하지 않는다.
- 실패하는 테스트를 이유 없이 삭제하거나 비활성화하지 않는다.
- 실행하지 않은 테스트를 실행했다고 말하지 않는다.
- 빌드하지 못했으면 빌드 성공이라고 말하지 않는다.
- `@DisplayName`은 자연스러운 한국어 behavior 중심으로 작성한다.
- unrelated test cleanup은 하지 않는다.

## 현재 정책 - 실행 기준

- 변경 범위에 맞는 focused test를 먼저 실행한다.
- 가능하면 전체 `.\gradlew.bat test`를 실행한다.
- Backend focused test 예: `.\gradlew.bat test --tests com.example.my_project_1.post.*`
- Backend full test 예: `.\gradlew.bat test`
- Backend compile 예: `.\gradlew.bat clean compileJava`
- frontend 수정 시 최소 `npm run build`를 실행한다.

## 현재 정책 - 테스트 우선순위

1. Domain unit test
2. Application service test
3. Outbox processor/handler/repository test
4. Security/JWT/filter test
5. Repository query test
6. Controller request/response test
7. Frontend build 또는 browser smoke test

## 현재 정책 - Redis 테스트

- Lua script 호출 여부와 실제 Redis 동작을 분리해 테스트할 수 있다.
- Redis integration test는 localhost Redis를 사용할 수 있다.
- localhost Redis가 없으면 JUnit assumption으로 skip될 수 있다.
- skip은 실제 Redis 동작 통과와 다르게 해석한다.
- Redis Cluster 동작은 현재 integration test의 기본 검증 범위가 아니다.

## 현재 정책 - Outbox 테스트

- claim 실패 시 handler가 실행되지 않는지 검증한다.
- handler 성공 시 `SUCCESS`, 실패 시 `FAILED` 또는 `DEAD` 전이를 검증한다.
- retry/backoff/jitter는 exact second에 의존하지 말고 range로 검증한다.
- stuck `PROCESSING` recovery를 검증한다.
- admin retry는 `resetForRetry` 정책을 검증한다.
- duplicate eventKey 정책은 일반 `publish`와 `publishIfAbsent`를 구분해 검증한다.

## 현재 정책 - Security/Auth 테스트

- login success/failure/count/block/email normalize를 검증한다.
- access token 만료, blacklist, invalid bearer 형식을 검증한다.
- refresh token 만료, type mismatch, hash mismatch를 검증한다.
- reissue cookie/header fallback과 mismatch를 검증한다.
- logout optional access token, expired access token, refresh token hash 검증을 검증한다.
- Security slice test는 `PasswordConfig` 등 필요한 config import 누락에 주의한다.
- `spring.main.allow-circular-references=true`로 테스트를 우회하지 않는다.

## 주의사항

- 테스트 fixture를 바꾸더라도 storageKey, eventKey, enum name, API path, JSON field name처럼 의미 있는 문자열은 함부로 바꾸지 않는다.
- build만 통과한 경우 실제 브라우저 상호작용은 별도 미검증으로 남긴다.
- 최근 green 상태는 새 변경 이후 자동으로 유효하지 않다.
