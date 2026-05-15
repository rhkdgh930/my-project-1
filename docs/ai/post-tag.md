# Post Tag

## 현재 정책

- 게시글 생성/수정 요청은 `tags`를 선택 필드로 받을 수 있다.
- `tags == null`이면 빈 목록처럼 처리한다.
- 태그명은 `trim`하고 빈 태그는 제거한다.
- 중복 판단은 `trim` 결과 기준으로 하며, 영문 대소문자 정규화는 아직 하지 않는다.
- 게시글당 태그는 최대 5개, 태그명은 최대 20자까지 허용한다.
- `Tag`는 `id`, `name`, `createdAt`을 가진다.
- `PostTag`는 `Post @ManyToOne`을 갖지 않고 `Long postId`, `Long tagId` ID 참조를 사용한다.
- 같은 게시글에 같은 태그가 중복 연결되지 않도록 `(post_id, tag_id)` unique constraint를 둔다.
- 목록/상세 응답에는 additive field로 `tags`를 포함한다.
- 목록/상세 태그 매핑은 postId 목록 기반 bulk 조회로 처리해 N+1을 피한다.
- 태그 검색과 태그별 게시글 조회는 이번 범위에서 제외한다.
- `Tag` 자체는 게시글 삭제나 태그 교체 때 즉시 삭제하지 않는다.

## TODO

- 영문 태그 대소문자 정규화 정책을 검토한다.
- 태그 검색과 태그별 게시글 조회 API를 별도 범위로 검토한다.
- 사용되지 않는 orphan `Tag` cleanup 배치를 검토한다.
