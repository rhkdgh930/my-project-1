# Board / Post Domain AI Rules

이 문서는 Board/Post 조회, soft delete, Redis count/like, Post image outbox 정책을 정의한다.

## 현재 정책

### Board / Post Hidden Soft Delete

- Board와 Post는 일반 사용자 조회에서 숨기는 hidden soft delete 모델을 사용한다.
- Board delete는 `deletedAt` 기반 soft delete다.
- Post delete는 `deletedAt` 기반 soft delete다.
- active post 조건은 `post.deletedAt IS NULL AND post.board.deletedAt IS NULL`이다.
- 삭제된 Board 아래 Post는 목록, 상세, 댓글, 좋아요, 수정, 삭제 대상에서 제외한다.
- 삭제된 Post는 일반 조회와 일반 delete 대상이 아니다.
- 관리자 삭제 정책이 필요하면 일반 delete를 넓히지 말고 별도 admin API로 분리한다.

### Post Delete / Outbox

- Post delete 성공 시 `Post.delete(now)`로 title/content masking과 `deletedAt` 설정을 수행한다.
- Post delete 성공 후 `POST_DELETED` Outbox event를 발행한다.
- `POST_DELETED` payload는 `postId`, `userId`를 가진다.
- `PostDeletedHandler`는 `imageService.syncImages(postId, POST, emptyList, userId)`를 호출해 기존 post image를 detach한다.

### Post Query

- 게시글 목록 조회는 `PostRepository.searchActivePosts(boardId, condition, pageable)` QueryDSL 쿼리를 사용한다.
- 검색 조건은 `keyword`, `searchType`, `sortType`이다.
- `searchType`은 `TITLE`, `CONTENT`, `TITLE_CONTENT`를 지원하며 기본값은 `TITLE_CONTENT`이다.
- `sortType`은 `LATEST`, `OLDEST`, `VIEW_COUNT`, `LIKE_COUNT`를 지원하며 기본값은 `LATEST`이다.
- `keyword`가 없거나 blank이면 검색 조건 없이 active post 목록을 조회한다.
- active 조건은 `post.deletedAt IS NULL AND post.board.deletedAt IS NULL`이다.
- 정렬 조건:
  - `LATEST`: `createdAt desc`, `id desc`
  - `OLDEST`: `createdAt asc`, `id asc`
  - `VIEW_COUNT`: DB `viewCount desc`, `id desc`
  - `LIKE_COUNT`: DB `likeCount desc`, `id desc`
- 조회수/좋아요 정렬은 Redis 최신값이 아니라 DB에 마지막 sync된 count 기준이다.
- 응답의 `viewCount`/`likeCount`는 기존처럼 Redis 최신값이 있으면 Redis 값으로 보정될 수 있다.

- `PostQueryServiceImpl.getPosts`는 board 존재와 삭제 여부를 먼저 검증한다.
- 빈 page에서도 `Page.empty(pageable)`로 조기 반환하지 않는다.
- repository가 반환한 원본 `Page<Post>`의 `page.map(...)` 흐름을 유지한다.
- authorIds가 비어 있으면 `UserClient.findAuthorsByIds(...)` 호출을 생략한다.
- `PageResponse` metadata는 원본 page 기준으로 유지한다.
- UserClient 실패 또는 누락된 author는 `AuthorSummary.unknown()` fallback을 사용한다.

### Redis Count / Like

- API 응답 count는 Redis 값이 있으면 Redis 값을 사용하고, 없으면 DB count로 fallback한다.
- view count와 like count fallback은 독립적으로 처리한다.
- view dirty marker와 like dirty marker는 분리되어 있다.
- legacy `post::dirty` 단일 set은 현재 sync 흐름에서 사용하지 않는다.
- `increaseView(postId)`는 Lua script로 view count 증가와 view dirty marker 등록을 원자적으로 처리한다.
- like toggle은 Lua script로 user like set, like count, like dirty marker를 원자적으로 처리한다.
- like count는 음수가 되지 않도록 Lua script에서 방어한다.

### PostSyncScheduler

- scheduler는 view dirty set과 like dirty set을 각각 처리한다.
- Redis count를 한 번 읽고 DB에 반영한다.
- DB update 이후 `removeViewDirtyIfUnchanged` 또는 `removeLikeDirtyIfUnchanged`를 호출한다.
- dirty 제거는 Lua script로 처리한다.
- Redis 현재 count가 DB에 반영한 `syncedCount`와 같을 때만 dirty marker를 제거한다.
- Redis 현재 count가 `syncedCount`와 다르면 sync 이후 새 증가/토글이 있었다고 보고 dirty marker를 유지한다.
- Redis count key가 null이면 DB update를 하지 않고 dirty marker도 제거하지 않는다.
- 이 정책은 데이터 유실보다 재시도와 운영 감지를 우선한다.
- DB update가 실패하면 dirty marker를 제거하지 않는다.
- invalid dirty id는 log 후 skip한다.

### Redis 운영 전제

- 현재 Redis count/like Lua script는 단일 Redis 또는 non-cluster Redis 전제다.
- Redis Cluster를 사용할 경우 multi-key Lua script의 hash slot 정책을 검토해야 한다.

## 테스트 기준

- active post query가 삭제된 post와 삭제된 board 아래 post를 제외하는지 검증한다.
- Post create/update/delete Outbox event type, eventKey, payload shape를 검증한다.
- Post image sync payload는 `/images/{uuid}.{ext}` storageKey만 포함하는지 검증한다.
- empty page에서 `UserClient` 호출을 생략하고 `PageResponse` metadata를 유지하는지 검증한다.
- Redis count fallback을 검증한다.
- `increaseView` Lua 실행과 실제 Redis count/dirty marker 갱신을 검증한다.
- `removeDirtyIfUnchanged`는 exact timing이 아니라 count match/mismatch로 검증한다.
- scheduler는 count null 시 dirty marker를 유지하는지 검증한다.

## TODO / 운영 전 개선

- `CONTENT` 검색은 `@Lob` 본문에 LIKE 검색을 수행하므로 대량 데이터에서는 full-text index 또는 search engine 도입을 검토한다.

- Redis count key 유실이 반복되면 dirty marker가 계속 남을 수 있다.
- stale dirty marker 감지를 위한 운영 로그, metric, admin 진단 기능을 보강한다.
- Redis Cluster 전환 전 key hash tag 설계를 검토한다.
- 좋아요의 감사/복구 정확도가 더 중요해지면 DB `post_likes` 영속화 전환을 검토한다.
