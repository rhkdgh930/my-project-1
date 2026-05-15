# Operations AI Rules

## 최신 TODO - Report / Moderation / Tag

- 신고 `content`에는 민감정보가 포함될 수 있으므로 관리자 화면 masking 정책을 검토한다.
- 신고 조치 이력과 관리자 audit log가 필요하면 별도 audit 정책으로 분리한다.
- 태그 검색, 태그별 게시글 조회, orphan `Tag` cleanup은 운영/기능 확장 시 별도 범위로 검토한다.
- 운영 DB migration 도입 시 `tag`, `post_tag`, `unique(post_id, tag_id)`와 필요한 FK를 명시한다.

이 문서는 운영/공개 전 TODO를 모은다. 현재 구현 정책이 아니라 배포 전 점검 목록이다.

## GitHub 공개 전

- 실제 secret 값을 repository, README, docs, sample에 남기지 않는다.
- JWT secret, OAuth2 secret, mail password, DB credential은 환경 변수 또는 secret store로 분리한다.
- `application-prod.yml`에 위험한 기본값이 없는지 확인한다.
- 운영 profile에서 `ddl-auto: create-drop`과 불필요한 `show-sql`을 제거한다.
- README에는 실행 방법, 환경 변수, 테스트 방법, Swagger 경로를 명확히 적는다.
- GitHub Actions CI를 추가해 compile/test를 자동 검증한다.
- Docker Compose로 MySQL/Redis/local app 실행 구성을 정리한다.

## 실제 배포 전

- 운영 cookie의 `secure`, `sameSite`, `domain`, `path`를 확정한다.
- CORS allowed origins를 운영 도메인 기준으로 제한한다.
- OAuth2 redirect URL을 운영 도메인 기준으로 분리한다.
- OAuth2 success redirect의 access token query param 방식은 장기적으로 제거한다.
- 5MB 초과 multipart upload 예외 응답을 controller/integration test로 확인한다.
- 5MB multipart 초과 응답을 명확한 API error shape로 고정한다.

## DB / Migration TODO

- 운영 배포 전 Flyway 또는 Liquibase 같은 DB migration 도구 도입을 검토한다.
- schema 변경은 migration script로 관리한다.
- `PostLike`가 ID 참조 구조라 Hibernate auto-DDL만으로는 `post_like.post_id -> post.id` DB FK가 자동 생성되지 않을 수 있다.
- migration 도입 시 `post_like.post_id -> post.id` FK를 명시한다.
- 더 강한 race 방어가 필요하면 like write query에 active post 조건을 포함하는 방안을 검토한다.
- DB별 duplicate 처리 최적화가 필요하면 `post_like` insert에 native upsert 또는 insert-ignore를 검토한다.
- 운영 DB에서는 destructive ddl-auto 설정을 사용하지 않는다.

## Redis TODO

- Redis Cluster 사용 여부를 결정한다.
- Cluster 사용 시 Auth refresh rotation/reissue history Lua key hash slot 정책을 검토한다.
- Cluster 사용 시 Post view Lua key hash slot 정책을 검토한다.
- stale dirty marker count, Redis command failure를 metric/log/admin 진단 기능으로 보강한다.

## Outbox TODO

- SUCCESS event retention 기간을 운영 정책으로 정한다.
- cleanup scheduler 주기와 보관 기간을 환경별로 조정할 수 있게 검토한다.
- email exactly-once는 현재 미보장이다.
- email 중복 발송을 줄이기 위한 dedupe key 또는 발송 이력 정책을 검토한다.
- handler별 retryable/non-retryable exception 분류를 강화할 수 있다.

## Image TODO

- profile image 교체 시 이전 이미지 cleanup 정책을 설계한다.
- profile image cleanup 대상/실패 count를 관찰한다.
- 파일 삭제 cleanup 실패를 metric/log/admin 진단 기능으로 보강한다.

## Observability TODO

- stale dirty marker count
- Outbox `PENDING`/`FAILED`/`DEAD`/`PROCESSING` count
- stuck `PROCESSING` recovery count
- login failure/block count
- Redis command failure
- file delete cleanup failure
- OAuth2 failure reason
