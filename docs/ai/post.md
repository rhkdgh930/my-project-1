# Board / Post AI Rules

## 최신 정책 - Tag Posts

- 태그별 게시글 조회 API는 `GET /api/tags/{tagName}/posts`다.
- Public API이며 `tagName`은 `trim` 기준으로 조회한다.
- 현재 영문 대소문자 정규화는 하지 않으므로 `Java`와 `java`는 다른 태그다.
- 존재하지 않는 태그명은 에러가 아니라 빈 `PageResponse<PostListResponse>`를 반환한다.
- 삭제된 게시글과 삭제된 게시판 아래 게시글은 제외한다.
- 정렬은 `post.createdAt desc`, `post.id desc` 최신순이다.
- 응답 `viewCount`는 기존 목록 응답처럼 `DB viewCount + Redis delta`로 보정한다.
- 태그별 게시글 조회는 `viewCount`를 증가시키지 않는다.
- 응답의 `tags`는 기존 정책대로 postId 목록 기반 bulk 조회로 채운다.

## 최신 정책 - Post Tag

- 게시글 생성/수정 요청은 optional `tags`를 받을 수 있다.
- `tags == null`이면 빈 목록처럼 처리한다.
- 태그명은 `trim`하고 빈 태그는 제거한다.
- 중복 판단은 `trim` 결과 기준이며, 영문 대소문자 정규화는 아직 하지 않는다.
- 게시글당 태그는 최대 5개, 태그명은 최대 20자까지 허용한다.
- `Tag`는 `id`, `name`, `createdAt`을 가진다.
- `PostTag`는 `id`, `postId`, `tagId`를 가지며 `Post @ManyToOne` 없이 ID 참조 구조를 사용한다.
- `unique(post_id, tag_id)`로 같은 게시글 중복 태그를 방어한다.
- 목록/상세/인기글/마이페이지 목록 응답에 additive field `tags`를 노출한다.
- 태그 조회는 postId 목록 기반 bulk 조회로 처리해 N+1을 피한다.

## TODO - Post Tag

- 태그 자동완성, 태그 목록 API, 복합 태그 검색은 별도 범위로 검토한다.
- 영문 태그 대소문자 정규화 정책을 검토한다.
- 사용되지 않는 orphan `Tag` cleanup을 검토한다.

이 문서는 Board/Post 조회, hidden soft delete, QueryDSL 검색, Redis view count, DB like, Post image Outbox 정책을 정리한다.

## 현재 정책 - Board / Post Soft Delete

- Board와 Post는 일반 사용자 조회에서 숨기는 hidden soft delete 모델을 사용한다.
- Board delete와 Post delete는 `deletedAt` 기반 soft delete다.
- Post delete는 `title`/`content`를 마스킹한다.
- active post 조건은 `post.deletedAt IS NULL AND post.board.deletedAt IS NULL`이다.
- 삭제된 Board 아래 Post는 목록, 상세, 댓글, 좋아요, 수정, 삭제 대상에서 제외한다.
- 관리자 삭제 조회가 필요하면 일반 조회 정책을 넓히지 말고 별도 admin API로 분리한다.

## 현재 정책 - Post Delete / Image Outbox

- Post delete 성공 시 `Post.delete(now)`로 마스킹과 `deletedAt` 설정을 수행한다.
- Post delete 성공 시 `POST_DELETED` Outbox event를 발행한다.
- `POST_DELETED` payload는 `postId`, `userId`를 가진다.
- `PostDeletedHandler`는 `imageService.syncImages(postId, POST, emptyList, userId)`를 호출해 기존 post image를 detach한다.

## 현재 정책 - QueryDSL 검색/정렬

- 게시글 목록 조회는 `PostRepository.searchActivePosts(boardId, condition, pageable)` QueryDSL 쿼리를 사용한다.
- 검색 조건은 `keyword`, `searchType`, `sortType`이다.
- `searchType`은 `TITLE`, `CONTENT`, `TITLE_CONTENT`를 지원하고 기본값은 `TITLE_CONTENT`다.
- `sortType`은 `LATEST`, `OLDEST`, `VIEW_COUNT`, `LIKE_COUNT`를 지원하고 기본값은 `LATEST`다.
- `keyword`가 null 또는 blank이면 검색 조건 없이 active post 목록을 조회한다.
- 정렬 기준은 `LATEST`: `createdAt desc, id desc`, `OLDEST`: `createdAt asc, id asc`, `VIEW_COUNT`: `viewCount desc, id desc`, `LIKE_COUNT`: `likeCount desc, id desc`이다.
- 정렬은 DB에 마지막으로 sync된 `viewCount`와 DB `likeCount` 기준이다.
- `CONTENT` 검색은 큰 데이터에서 full-text index 또는 search engine 도입을 검토한다.

## 현재 정책 - Popular Posts

- 인기글 조회 API는 `GET /api/boards/{boardId}/posts/popular`이다.
- Public API이며 삭제된 Board/Post는 제외한다.
- 초기 점수는 DB 기준 `likeCount * 3 + viewCount`이다.
- Redis view delta는 ranking 계산에 반영하지 않는다.
- 응답의 `viewCount`는 기존 목록 응답처럼 `DB viewCount + Redis delta`로 보정한다.
- 조회 호출 자체는 `viewCount`를 증가시키지 않는다.
- 기본 size는 10이고 최대 50으로 제한한다.

## 현재 정책 - Redis View Count

- Redis는 view count 전용으로 사용한다.
- Redis에는 DB 미반영 조회수 증가분만 저장한다.
- delta key는 `post::view::delta::{postId}`이고 dirty set은 `post::dirty::view`이다.
- 상세 조회 시 `increaseView(postId)`가 Lua script로 delta `INCR`과 dirty `SADD`를 원자 처리한다.
- 목록/상세 응답 viewCount는 `DB viewCount + Redis delta`로 계산한다.
- Redis delta가 없으면 DB count를 그대로 사용한다.
- scheduler는 dirty id마다 Redis view delta를 읽고 DB `viewCount`에 누적 반영한다.
- DB update 성공 후 `acknowledgeSyncedViewDelta(postId, syncedDelta)`로 syncedDelta만큼 안전하게 차감한다.
- DB update 실패 시 Redis delta와 dirty marker를 유지한다.

## 현재 정책 - Like

- `post_like` table이 좋아요 여부의 source of truth다.
- `post_like`는 `post_id`, `user_id`, `created_at`을 가지며 `(post_id, user_id)` unique constraint가 필요하다.
- `PostLike`는 `Post @ManyToOne`을 갖지 않고 `Long postId`, `Long userId`, `createdAt`을 저장한다.
- `PostLike`는 Post aggregate 내부 구성요소가 아니라 `userId-postId` 좋아요 행위 기록으로 본다.
- Post 존재, active post 정책, board-post 관계 검증은 `PostCommandService`에서 수행한다.
- 좋아요 API는 멱등 `PUT /like`, `DELETE /like`만 사용한다.
- legacy `POST /like` toggle API는 제거됐다.
- `PUT /api/boards/{boardId}/posts/{postId}/like`는 이미 좋아요 상태여도 성공하고 `PostLikeResponse(liked=true, likeCount)`를 반환한다.
- `DELETE /api/boards/{boardId}/posts/{postId}/like`는 이미 취소 상태여도 성공하고 `PostLikeResponse(liked=false, likeCount)`를 반환한다.
- 좋아요 경로에서는 `PESSIMISTIC_WRITE`를 사용하지 않는다.
- 중복 좋아요는 `(post_id, user_id)` unique constraint로 방어한다.
- `PUT /like`는 `post_like` insert 성공 시에만 `Post.likeCount`를 +1 한다. unique 충돌은 이미 좋아요 상태로 보고 count를 변경하지 않는다.
- `DELETE /like`는 `deleteByPostIdAndUserId` affected row가 1 이상일 때만 `Post.likeCount`를 -1 한다.
- `Post.likeCount`는 denormalized DB count다.
- Redis like user set, Redis like count, like dirty marker, like sync는 사용하지 않는다.
- 좋아요 API 호출은 `viewCount`를 증가시키지 않는다.
- 상세 응답의 `likedByMe`는 로그인 사용자의 `post_like` row 존재 여부 기준이며, 비로그인 사용자는 `false`다.

## 주의사항

- Redis like 구조나 Redis view absolute sync를 현재 정책처럼 복구하지 않는다.
- `PostLike`를 `Post @ManyToOne` 구조로 되돌리지 않는다.
- `POST /like` toggle을 신규 API처럼 다시 문서화하지 않는다.
- image lifecycle 세부 상태는 `docs/ai/image.md`를 따른다.

## 테스트 기준

- active post query가 삭제된 post와 삭제된 board 아래 post를 제외하는지 검증한다.
- Redis view delta 보정과 scheduler acknowledge 정책을 검증한다.
- 좋아요 row insert/delete, unique constraint, likeCount atomic delta update, 음수 방어를 검증한다.
- `POST /like` toggle API가 controller, Swagger, client code에 남아 있지 않은지 검증한다.
