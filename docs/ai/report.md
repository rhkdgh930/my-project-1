# Report

## 최신 정책 - 신고 기반 명시 조치

- `ACTION_TAKEN`은 관리자가 조치를 완료했다고 표시하는 상태값이며, 상태 변경 API 자체가 자동 삭제/자동 정지를 수행하지 않는다.
- `PATCH /api/admin/reports/{reportId}/status`는 신고 상태만 변경한다.
- 신고 기반 명시 조치 API는 `POST /api/admin/reports/{reportId}/actions/delete-target`와 `POST /api/admin/reports/{reportId}/actions/suspend-user`다.
- `delete-target`은 `POST` 신고면 게시글 삭제 후 `ACTION_TAKEN`, `COMMENT` 신고면 댓글 삭제 후 `ACTION_TAKEN`으로 변경한다.
- `delete-target`은 `USER` 신고를 지원하지 않는다.
- `suspend-user`는 `USER` 신고만 지원하며, 유저 정지 후 `ACTION_TAKEN`으로 변경한다.
- `suspend-user`를 `POST`/`COMMENT` 신고에 호출하면 `UNSUPPORTED_REPORT_TARGET`로 실패한다.
- `suspend-user`는 관리자 자기 자신 정지를 금지한다.
- `suspend-user`의 정지 정책은 기존 `AdminUserCommandService.suspendUser` 정책을 재사용한다.
- 기존 직접 moderation API인 `DELETE /api/admin/moderation/posts/{postId}`, `DELETE /api/admin/moderation/comments/{commentId}`는 신고와 무관한 관리자 직접 조치용으로 유지한다.

## 최신 정책 - USER 신고 정지 조치

- `POST /api/admin/reports/{reportId}/actions/suspend-user`는 `USER` 신고 기반 명시 정지 조치 API다.
- 이 API는 대상 유저를 기존 Admin User 정지 정책으로 정지한 뒤 신고 상태를 `ACTION_TAKEN`으로 변경한다.
- `ReportService.updateStatus`와 `PATCH /api/admin/reports/{reportId}/status`는 여전히 상태만 변경하며 자동 정지를 수행하지 않는다.
- `POST`/`COMMENT` 신고는 `suspend-user`에서 지원하지 않고 `UNSUPPORTED_REPORT_TARGET`로 실패한다.
- 관리자가 자기 자신을 신고 기반 조치로 정지하는 것은 금지한다.
- 이미 정지된 유저, 탈퇴/삭제 유저 처리 정책은 기존 Admin User 정지 정책을 재사용한다.

## 최신 정책 - 신고 기반 통합 조치

- `Report` 도메인은 신고 접수와 관리자 검토 상태 관리만 담당한다.
- `ACTION_TAKEN`은 자동 삭제/자동 정지를 수행하지 않는 상태값이다.
- `POST /api/admin/reports/{reportId}/actions/delete-target`는 신고 기반 명시 조치 API다.
- `POST` 신고는 게시글 삭제 후 `ACTION_TAKEN`으로 변경한다.
- `COMMENT` 신고는 댓글 삭제 후 `ACTION_TAKEN`으로 변경한다.
- `USER` 신고는 이 API에서 지원하지 않고 기존 Admin User 화면에서 처리하도록 안내한다.
- `DELETE /api/admin/moderation/posts/{postId}`와 `DELETE /api/admin/moderation/comments/{commentId}`는 신고와 무관한 관리자 직접 조치용으로 유지한다.
- 신고 `content` 민감정보 masking과 audit log는 TODO다.

## 현재 정책

- `Report` 도메인은 신고 접수와 관리자 검토 상태 관리만 담당한다.
- 신고 대상은 `POST`, `COMMENT`, `USER`이며, 같은 사용자의 같은 대상 반복 신고는 `unique(reporter_id, target_type, target_id)`로 막는다.
- 삭제된 게시글과 삭제된 댓글은 신고할 수 없다.
- 자기 자신에 대한 `USER` 신고는 허용하지 않는다.
- 신고 상태는 `PENDING`, `REVIEWED`, `REJECTED`, `ACTION_TAKEN`을 사용한다.
- `ACTION_TAKEN`은 자동 삭제나 자동 정지를 수행하는 트리거가 아니라, 관리자가 조치를 완료했다고 표시하는 상태값이다.

## API 정책

- 사용자 신고 생성은 `POST /api/reports`를 사용하며 인증이 필요하다.
- 관리자 신고 목록/상세/상태 변경은 `GET /api/admin/reports`, `GET /api/admin/reports/{reportId}`, `PATCH /api/admin/reports/{reportId}/status`를 사용하며 `ADMIN` 권한이 필요하다.
- `PATCH /api/admin/reports/{reportId}/status`는 신고 상태만 변경하고 신고 대상을 변경하지 않는다.
- 신고 기반 명시 조치는 `POST /api/admin/reports/{reportId}/actions/delete-target`를 사용한다.
- `delete-target`은 `POST` 신고면 게시글을 삭제하고, `COMMENT` 신고면 댓글을 삭제한 뒤 신고 상태를 `ACTION_TAKEN`으로 변경한다.
- `delete-target`은 `USER` 신고를 지원하지 않는다. 사용자 정지는 기존 관리자 사용자 관리 기능으로 처리한다.
- `DELETE /api/admin/moderation/posts/{postId}`와 `DELETE /api/admin/moderation/comments/{commentId}`는 신고와 무관한 관리자 직접 조치용으로 유지한다.

## 주의사항

- `ReportService.updateStatus`에 자동 삭제/정지 로직을 넣지 않는다.
- 신고 기반 조치가 필요하면 service 계층의 통합 유스케이스에서 대상 조치와 신고 상태 변경을 한 트랜잭션으로 묶는다.
- `Report` 도메인이 `Post`, `Comment`, `User` 도메인을 직접 강하게 소유하지 않도록 조율 책임은 application/service 계층에 둔다.

## TODO

- 신고 payload/content에 민감정보가 포함될 수 있으므로 관리자 화면 마스킹 정책을 검토한다.
- 신고 조치 이력과 감사 로그가 필요해지면 별도 audit 정책으로 분리한다.
- `ADMIN` 계정 정지 금지 여부를 운영 정책으로 명확히 정한다.
