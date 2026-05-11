# Testing AI Rules

이 문서는 테스트 작성과 실행 기준을 정의한다.

## 기본 원칙

- 변경한 behavior는 테스트로 검증한다.
- 테스트를 통과시키기 위해 실제 정책을 약화하지 않는다.
- 실패하는 테스트를 이유 없이 삭제하거나 비활성화하지 않는다.
- 실행하지 않은 테스트를 실행했다고 말하지 않는다.
- 빌드하지 못했으면 빌드 성공이라고 말하지 않는다.
- 테스트 DisplayName은 가능하면 한국어 behavior 중심으로 작성한다.
- unrelated test cleanup은 하지 않는다.

## 우선순위

1. Domain unit test
2. Application service test
3. Outbox processor/handler/repository test
4. Security/JWT/filter test
5. Repository query test
6. Controller request/response test
7. Frontend build 또는 e2e smoke test

## 실행 명령

Backend compile:

```powershell
.\gradlew.bat compileJava
```

Backend focused test:

```powershell
.\gradlew.bat test --tests com.example.my_project_1.some.TestClass
```

Backend full test:

```powershell
.\gradlew.bat test
```

Frontend build:

```powershell
npm run build
```

## Redis Integration Test

- Redis integration test는 localhost Redis를 사용할 수 있다.
- localhost Redis가 없으면 JUnit assumption으로 skip될 수 있다.
- skip된 테스트는 실제 Redis 동작 검증으로 간주하지 않는다.
- Redis Cluster 동작은 현재 integration test의 기본 검증 범위가 아니다.

## Outbox Test

- claim 실패 시 handler가 실행되지 않는지 검증한다.
- handler 성공 시 `SUCCESS`, 실패 시 `FAILED` 또는 `DEAD` 전이를 검증한다.
- retry/backoff/jitter는 exact second에 의존하지 말고 범위로 검증한다.
- stuck PROCESSING recovery를 검증한다.
- admin retry는 `resetForRetry` 정책을 검증한다.
- duplicate eventKey 정책은 일반 `publish`와 `publishIfAbsent`를 구분해 검증한다.

## Security/Auth Test

- login success/failure/count/block/email normalize를 검증한다.
- access token 만료, blacklist, invalid bearer 형식을 검증한다.
- refresh token 만료, type mismatch, hash mismatch를 검증한다.
- reissue cookie/header fallback과 mismatch를 검증한다.
- logout optional access token, expired access token, refresh token hash 검증을 검증한다.
- Security slice test는 필요한 config import, 특히 PasswordEncoder config 누락에 주의한다.
- `spring.main.allow-circular-references=true`로 테스트를 우회하지 않는다.

## Post/Image Test

- active post query는 deleted post/board를 제외해야 한다.
- Redis count fallback과 Lua script 기반 count/dirty 갱신을 검증한다.
- dirty 제거는 `removeDirtyIfUnchanged` 기준으로 검증한다.
- Redis count null 시 dirty 유지 정책을 검증한다.
- Image parser는 strict URL parsing을 검증한다.
- Image attach는 distinct, idempotency, 상태 위반을 검증한다.

## Frontend Test / Build

- frontend 변경 후 최소 `npm run build`를 실행한다.
- Auth interceptor, route guard, admin 권한 UI는 가능하면 browser smoke test를 추가한다.
- build만 통과한 경우 실제 브라우저 상호작용은 별도 미검증으로 남긴다.

## 최근 Green 기준

- 최근 전체 backend `.\gradlew.bat test` green 상태를 확인했다.
- 최근 frontend `npm run build` green 상태를 확인했다.
- 단, 이후 변경이 있으면 다시 검증해야 한다.
