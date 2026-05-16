# Project AI Rules

## 최신 정책 요약 - Report / Moderation / Tag

- `Report` 도메인은 신고 접수와 관리자 검토 상태 관리만 담당한다.
- `ACTION_TAKEN`은 자동 삭제/자동 정지를 수행하지 않는 상태값이다.
- 신고 기반 명시 조치는 `POST /api/admin/reports/{reportId}/actions/delete-target`와 `POST /api/admin/reports/{reportId}/actions/suspend-user`를 사용한다.
- 기존 직접 moderation API인 `DELETE /api/admin/moderation/posts/{postId}`, `DELETE /api/admin/moderation/comments/{commentId}`는 유지한다.
- 게시글은 생성/수정 시 optional `tags`를 받을 수 있고, 목록/상세/인기글/마이페이지 목록 응답에 additive field `tags`를 노출한다.
- 태그별 게시글 조회는 `GET /api/tags/{tagName}/posts`로 제공하며, 태그 자동완성/목록 API와 신고 content masking/audit log는 TODO다.

이 문서는 프로젝트 전체를 수정할 때 지켜야 하는 상위 기준이다. 세부 정책은 각 도메인 문서를 우선한다.

## 현재 정책

- 목표는 단순 CRUD 게시판이 아니라 인증, 사용자 생명주기, 게시글/댓글, 이미지, Outbox, Redis, 운영 TODO를 갖춘 백엔드 중심 포트폴리오다.
- Backend는 Spring Boot 기반 layered architecture를 사용한다.
- Frontend는 React + TypeScript 기반이며 Backend API contract를 따른다.
- Auth/User/Post/Comment/Image/Outbox/Common은 각각 독립된 정책 문서를 가진다.
- Redis는 인증 보조와 post view count delta에 사용한다. 좋아요에는 사용하지 않는다.
- Outbox는 트랜잭션 이후 side effect를 안정적으로 처리하는 경계다.

## 주의사항

- API path, enum, DTO field, error code, event type은 호환성을 고려해 변경한다.
- 조회 정책과 쓰기 정책을 한 작업에서 섞어 바꾸지 않는다.
- 인증, 사용자 상태, soft delete, 이미지 lifecycle, Outbox side effect는 관련 문서를 먼저 확인한다.
- production 정책을 바꾸면 테스트와 문서를 함께 갱신한다.
- 테스트 통과만을 위해 도메인 정책을 약화하지 않는다.
- unrelated cleanup은 하지 않는다.

## TODO

- 공개 전에는 `README.md`, 실행 방법, 환경 변수, Swagger 경로, Docker/CI 문서를 별도로 정리한다.
- 운영/배포 관련 TODO는 `docs/ai/operations.md`를 기준으로 관리한다.
