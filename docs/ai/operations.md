# Operations AI Rules

이 문서는 운영/공개 전 TODO를 모은다. 현재 구현 정책이 아니라 배포 전 점검 목록이다.

## 반드시 해야 할 것

- secret/env/profile 정리
- 실제 secret 값을 repository, README, docs, sample에 남기지 않기
- `application-prod.yml` 안전화
- 운영 profile에서 `ddl-auto: create-drop` 제거
- 운영 profile에서 불필요한 `show-sql` 제거
- JWT secret, OAuth2 secret, mail password, DB credential을 환경 변수 또는 secret store로 분리
- 운영 cookie `secure`, `sameSite`, `domain`, `path` 확정
- OAuth2 redirect URL을 운영 도메인 기준으로 분리
- 5MB 초과 multipart upload 예외 응답 보강

## 보안 TODO

- OAuth2 success redirect의 access token query param 방식을 장기적으로 제거한다.
- 목표는 refresh token HttpOnly cookie 기반 reissue로 access token을 획득하는 구조다.
- refresh token fallback header는 운영에서 유지할지 제거할지 결정한다.
- CORS allowed origins를 운영 도메인 기준으로 제한한다.

## Redis TODO

- Redis Cluster 사용 여부를 결정한다.
- Cluster 사용 시 Auth refresh rotation/reissue history Lua key hash slot 정책을 검토한다.
- Cluster 사용 시 Post view/like Lua multi-key hash slot 정책을 검토한다.
- Redis count key 유실이 반복될 때 stale dirty marker를 감지할 metric/log/admin 진단 기능을 보강한다.

## Outbox TODO

- SUCCESS event retention 기간을 운영 정책으로 확정한다.
- cleanup scheduler 주기와 보존 기간을 환경별로 조정할 수 있는지 검토한다.
- email 발송 exactly-once는 현재 미보장이다.
- email 중복 발송을 줄이기 위한 dedupe key 또는 발송 이력 정책을 검토한다.
- handler별 retryable/non-retryable exception 분류를 강화할 수 있다.

## DB / Migration TODO

- 운영 배포 전 DB migration 도구 도입을 검토한다.
- schema 변경은 migration script로 관리한다.
- 운영 DB에는 destructive ddl-auto 설정을 사용하지 않는다.

## Observability TODO

- stale dirty marker count
- Outbox PENDING/FAILED/DEAD/PROCESSING count
- stuck PROCESSING recovery count
- login failure/block count
- Redis command failure
- file delete cleanup failure
- OAuth2 failure reason

## 확인 필요

- multipart max size 초과가 현재 어떤 예외 응답으로 내려가는지 실제 controller/integration test로 확인 필요.
- 실제 Google OAuth2 provider 연동은 local mock test와 별개로 staging에서 확인 필요.
