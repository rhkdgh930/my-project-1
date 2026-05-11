# Comment Domain AI Rules

이 문서는 comment/reply 작성, 수정, 삭제, tombstone 표시 정책을 정의한다.

## 현재 정책

### Endpoint / Security

- Comment 조회 API는 public GET 대상이다.
- Comment 작성, reply 작성, 수정, 삭제는 authenticated API다.
- 일반 Comment 수정/삭제는 작성자만 허용한다.
- 관리자 Comment 수정/삭제가 필요하면 별도 admin API로 분리한다.
- Comment public path를 method 구분 없이 broad wildcard로 열지 않는다.

### Comment Model

- Comment는 tombstone 모델을 사용한다.
- depth는 0 comment와 1 reply까지만 허용한다.
- reply의 parent는 같은 post에 속해야 한다.
- URL `postId`와 comment/reply의 postId는 일치해야 한다.
- 삭제된 Post 또는 삭제된 Board 아래 Post에는 comment/reply를 작성할 수 없다.
- 수정/삭제도 active post에서만 허용한다.

### Tombstone Delete

- Comment delete는 물리 삭제가 아니라 tombstone 처리다.
- 삭제 시 `deletedAt`을 설정한다.
- 삭제된 comment 응답은 `deleted=true`를 포함한다.
- 삭제된 comment의 content는 `"삭제된 댓글입니다."`로 표시한다.
- 삭제된 comment의 `authorId`는 null이다.
- 삭제된 comment의 `author`는 null이다.
- 삭제된 parent comment 아래 기존 reply는 계속 조회된다.
- 삭제된 comment에는 수정할 수 없다.
- 삭제된 comment에는 reply를 작성할 수 없다.
- 같은 작성자가 이미 삭제된 comment를 다시 삭제하면 idempotent하게 성공 처리한다.
- 다른 사용자의 comment 삭제 시도는 `ACCESS_DENIED` 정책을 따른다.

### Update / Delete Response

- PATCH 성공 응답은 수정된 `CommentResponse`다.
- DELETE 성공 응답은 `204 No Content`다.
- update/delete API는 현재 controller에 노출되어 있는 기능이다.

### Validation / Invariant

- create/reply/update request는 `@Valid`를 적용한다.
- Comment content는 domain level에서도 null, blank, 1000자 초과를 거부한다.
- depth 1 comment에는 reply를 작성할 수 없다.
- deleted parent comment에는 reply를 작성할 수 없다.
- URL postId와 parent/comment postId가 다르면 거부한다.

### AuthorSummary

- non-deleted comment 응답은 작성자 표시용 `AuthorSummary`를 사용한다.
- `UserClient.findAuthorsByIds(...)` 실패 또는 누락된 user는 `UNKNOWN` fallback을 사용한다.
- deleted comment는 tombstone 정책이 우선이므로 `author=null`을 유지한다.

## 테스트 기준

- create/reply/update request validation을 검증한다.
- 다른 post의 parent comment에 reply가 달리지 않는지 검증한다.
- depth 1 comment와 deleted comment에는 reply가 불가능한지 검증한다.
- deleted comment가 tombstone 형태로 조회되는지 검증한다.
- deleted post 또는 deleted board 아래 post에는 comment 작성/수정/삭제가 불가능한지 검증한다.
- update/delete에서 URL postId와 comment.postId 불일치를 거부하는지 검증한다.
- content domain invariant가 create/update 경로 모두에서 유지되는지 검증한다.

## TODO / 운영 전 개선

- 관리자 comment moderation이 필요하면 별도 admin API와 audit 정책을 추가한다.
