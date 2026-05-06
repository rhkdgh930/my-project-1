# Comment Domain AI Rules

## 목적

Comment 문서는 댓글/대댓글 작성, 조회, tombstone 정책을 정리한다.

이 문서는 Comment 관련 작업에서 우선 참조한다.

---

## Security / Endpoint Policy

- Comment 조회 API만 `GET` 기준 permitAll 대상이다.
- Comment 작성과 대댓글 작성 API는 authenticated가 필요하다.
- `PATCH /api/posts/{postId}/comments/{commentId}`는 authenticated API다.
- `DELETE /api/posts/{postId}/comments/{commentId}`는 authenticated API다.
- 일반 Comment 수정/삭제는 작성자만 허용한다.
- 관리자 수정/삭제는 아직 미구현이며, 필요하면 별도 admin API로 분리할 수 있다.
- Comment 관련 public path를 method 구분 없이 broad wildcard로 열지 않는다.

---

## Comment 모델 정책

- Comment는 tombstone 모델을 사용한다.
- depth는 0 댓글과 1 대댓글까지만 허용한다.
- 대댓글의 parent는 같은 post에 속해야 한다.
- reply API는 URL의 `postId`와 parent comment의 `postId`가 같은지 검증해야 한다.
- 삭제된 Post 또는 삭제된 Board 아래 Post에는 댓글과 대댓글을 작성할 수 없다.
- 수정/삭제 API는 URL의 `postId`와 `comment.postId`가 같은지 검증해야 한다.
- 수정/삭제는 active post에서만 허용한다.
- 삭제된 Post 또는 삭제된 Board 아래 Post의 comment 수정/삭제는 막는다.

---

## Comment 삭제 정책

- Comment 삭제는 물리 삭제가 아니라 tombstone 처리다.
- 삭제 시 `deletedAt`을 세팅한다.
- 삭제 시 `content`는 `"삭제된 댓글입니다."`로 변경한다.
- 삭제 댓글은 조회 응답에 포함해 대댓글 트리를 유지한다.
- 삭제 댓글 응답은 `deleted=true`를 포함한다.
- 삭제 댓글의 author 정보는 숨긴다.
- 삭제 댓글에는 수정과 답글 작성을 허용하지 않는다.
- 삭제된 부모 댓글 아래 기존 대댓글은 계속 조회된다.
- 같은 작성자가 이미 삭제한 comment를 다시 삭제하면 idempotent하게 `204 No Content`로 처리한다.
- 남의 삭제된 comment 삭제 시도는 `ACCESS_DENIED`다.

---

## Comment Update / Delete 응답 정책

- PATCH 성공 응답은 수정된 `CommentResponse`를 반환한다.
- DELETE 성공 응답은 `204 No Content`다.
- deleted comment의 tombstone 조회 응답 정책은 update/delete API 추가 후에도 유지한다.

---

## Validation / Invariant

- 현재 Comment create/reply 요청에는 `@Valid`를 적용한다.
- Comment update 요청 body에도 `@Valid`를 적용한다.
- Comment content는 domain level에서도 `null`, blank, 1000자 초과를 거부한다.
- Controller `@Valid`는 API 입구 검증이고, domain invariant는 service/domain 직접 호출 경로까지 보호하는 규칙이다.
- 대댓글 작성 시 parent comment가 이미 depth 1이면 거부한다.
- 대댓글 작성 시 parent comment가 삭제된 상태면 거부한다.
- 대댓글 작성 시 parent comment가 URL의 postId와 다른 post에 속하면 거부한다.
- Comment update/delete domain/service 정책은 작성자 권한만 구현한다.
- 관리자 수정/삭제 정책을 둘 경우 별도 정책으로 명시적으로 추가해야 한다.

---

## Testing Policy

- 댓글 작성 request validation을 검증한다.
- 대댓글 작성 request validation을 검증한다.
- 댓글 수정 request validation을 검증한다.
- 다른 post의 parent comment에 reply가 달리지 않는지 검증한다.
- 삭제된 comment에는 reply가 불가능한지 검증한다.
- depth 1 comment에는 reply가 불가능한지 검증한다.
- deleted comment가 tombstone 형태로 조회되는지 검증한다.
- 삭제된 post 또는 삭제된 board 아래 post에는 comment 작성이 불가능한지 검증한다.
- 삭제된 post 또는 삭제된 board 아래 post에는 comment 수정/삭제가 불가능한지 검증한다.
- 수정/삭제 시 URL postId와 comment.postId 불일치가 거부되는지 검증한다.
- content domain invariant가 create/update 경로에서 유지되는지 검증한다.
---

## AuthorSummary Response Policy

Comment 응답의 작성자 표시는 `AuthorSummary`를 우선 사용한다.

- Comment 조회 응답에는 작성자 표시용 `author` 객체를 포함한다.
- `author`는 `id`, `displayName`, `status`를 가진다.
- `AuthorStatus` 값은 `ACTIVE`, `WITHDRAWN`, `SUSPENDED`, `UNKNOWN`이다.
- 기존 `authorId` 필드는 transition을 위해 유지한다.
- `ACTIVE`: `id=userId`, `displayName=nickname`, `status=ACTIVE`
- `WITHDRAWN`: 내부 마스킹 nickname을 노출하지 않고 `id=null`, `displayName="탈퇴한 사용자"`, `status=WITHDRAWN`
- `SUSPENDED`: `id=userId`, `displayName="차단된 사용자"`, `status=SUSPENDED`
- `UNKNOWN`: `id=null`, `displayName="알 수 없는 사용자"`, `status=UNKNOWN`
- User row가 없거나 `UserClient.findAuthorsByIds(...)`가 실패해도 Comment 조회 자체는 실패시키지 않고 non-deleted comment의 author를 `UNKNOWN`으로 fallback한다.
- deleted comment는 기존 tombstone 정책대로 `author=null`을 유지한다.
