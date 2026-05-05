# Post Domain AI Rules

## 목적

Post 문서는 Board/Post 조회, 작성, 이미지 동기화, Redis count/like 정책을 정리한다.

이 문서는 Post 관련 작업에서 우선 참조한다.

---

## Security / Endpoint Policy

- Post 조회 API만 `GET` 기준 permitAll 대상이다.
- Post 생성, 수정, 좋아요 API는 authenticated가 필요하다.
- 현재 PostController에는 delete API를 노출하지 않았다.
- Post 삭제 API를 추가할 경우 authenticated로 둔다.
- Post 관련 public path를 method 구분 없이 broad wildcard로 열지 않는다.
- `/api/admin/**`는 ADMIN 권한이 필요하다.

---

## Board / Post 삭제 정책

- Board와 Post는 일반 사용자 조회에서 숨기는 hidden soft delete 모델을 사용한다.
- Board 삭제는 `deletedAt` 기반 soft delete다.
- Post 삭제는 `deletedAt` 기반 soft delete다.
- active post 조건은 `post.deletedAt IS NULL AND post.board.deletedAt IS NULL`이다.
- 삭제된 Board 아래 Post는 목록, 상세, 댓글, 좋아요, 수정 대상에서 제외한다.
- Post 삭제 flow를 추가할 경우에도 active post 검증을 사용한다.
- Post 조회 query는 Board 삭제 상태를 함께 고려해야 한다.

---

## Post Outbox / Image Sync 정책

- Post create/update 이후 본문 Markdown에서 내부 이미지 storageKey를 파싱한다.
- 이미지 attach/sync는 Outbox side effect로 처리한다.
- `POST_CREATED` eventKey는 `POST_CREATED:{postId}`를 사용한다.
- `POST_CREATED`는 같은 post 생성 이벤트 중복 처리를 막는 deterministic key가 적절하다.
- `POST_UPDATED` eventKey는 `POST_UPDATED:{postId}:{uuid}`를 사용한다.
- `POST_UPDATED`는 수정마다 image sync 이벤트 처리가 필요하므로 `updatedAt` auditing 값에 의존하지 않는다.
- `POST_UPDATED` eventKey에 flush 전 JPA auditing 값을 사용하지 않는다.
- Outbox payload shape는 `postId`, `userId`, `storageKeys`를 유지한다.

---

## Redis Count / Like 정책

- Redis는 단일 Redis 또는 non-cluster Redis를 전제로 한다.
- Redis Cluster에서는 Lua multi-key hash slot 문제가 있을 수 있으므로 cluster 전환 전 key hash tag 설계를 검토한다.
- API 응답 count는 Redis 값이 있으면 Redis 값을 사용하고, 없으면 DB count로 fallback한다.
- view count와 like count fallback은 독립적으로 처리한다.
- Scheduler는 view dirty marker와 like dirty marker를 분리해 부분 sync한다.
- legacy `post::dirty` 단일 set은 새 sync 흐름에서 사용하지 않는다.
- view dirty marker는 view count sync 성공 시에만 제거한다.
- like dirty marker는 like count sync 성공 시에만 제거한다.
- Redis count key가 없거나 DB update가 실패하면 해당 dirty marker를 제거하지 않는다.
- Like toggle은 Redis Lua script로 원자화한다.
- Lua script 안에서 membership 변경, count 증감, like dirty mark를 함께 처리한다.
- dirty marker 제거와 count 변경 사이의 race는 남은 tradeoff다.
- 좋아요의 정확한 감사/복구가 중요해지면 DB `post_likes` 테이블 도입을 검토한다.

---

## Testing Policy

- active post query 변경은 삭제된 post와 삭제된 board 아래 post가 제외되는지 검증한다.
- Post create/update Outbox 테스트는 event type, eventKey, payload shape를 검증한다.
- Post image sync payload 테스트는 `/images/{uuid}.{ext}` 내부 URL만 포함되는지 검증한다.
- 외부 이미지 URL은 storageKeys payload에 포함되지 않아야 한다.
- Redis count fallback, dirty marker sync, Lua toggle은 focused unit test와 실제 Redis integration test를 함께 고려한다.
