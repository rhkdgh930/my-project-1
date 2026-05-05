# Comment Domain AI Rules

## 목적

Comment 문서는 댓글/대댓글 작성, 조회, tombstone 정책을 정리한다.

이 문서는 Comment 관련 작업에서 우선 참조한다.

---

## Security / Endpoint Policy

- Comment 조회 API만 `GET` 기준 permitAll 대상이다.
- Comment 작성과 대댓글 작성 API는 authenticated가 필요하다.
- 현재 CommentController에는 작성, 대댓글 작성, 조회 API만 노출되어 있다.
- Comment service/domain에는 update/delete가 존재하지만 controller에는 아직 노출하지 않았다.
- 수정/삭제 API를 controller에 노출할 경우 authenticated로 둔다.
- Comment 관련 public path를 method 구분 없이 broad wildcard로 열지 않는다.

---

## Comment 모델 정책

- Comment는 tombstone 모델을 사용한다.
- depth는 0 댓글과 1 대댓글까지만 허용한다.
- 대댓글의 parent는 같은 post에 속해야 한다.
- reply API는 URL의 `postId`와 parent comment의 `postId`가 같은지 검증해야 한다.
- 삭제된 Post 또는 삭제된 Board 아래 Post에는 댓글과 대댓글을 작성할 수 없다.

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

---

## Validation / Invariant

- 현재 Comment create/reply 요청에는 `@Valid`를 적용한다.
- update API를 추가할 경우 update request body에도 `@Valid`를 적용한다.
- 대댓글 작성 시 parent comment가 이미 depth 1이면 거부한다.
- 대댓글 작성 시 parent comment가 삭제된 상태면 거부한다.
- 대댓글 작성 시 parent comment가 URL의 postId와 다른 post에 속하면 거부한다.
- 현재 Comment update/delete domain/service 정책은 작성자 권한만 구현한다.
- 관리자 수정/삭제 정책은 현재 구현하지 않았다.
- 관리자 수정/삭제 정책을 둘 경우 별도 정책으로 명시적으로 추가해야 한다.

---

## Testing Policy

- 댓글 작성 request validation을 검증한다.
- 대댓글 작성 request validation을 검증한다.
- 다른 post의 parent comment에 reply가 달리지 않는지 검증한다.
- 삭제된 comment에는 reply가 불가능한지 검증한다.
- depth 1 comment에는 reply가 불가능한지 검증한다.
- deleted comment가 tombstone 형태로 조회되는지 검증한다.
- 삭제된 post 또는 삭제된 board 아래 post에는 comment 작성이 불가능한지 검증한다.
