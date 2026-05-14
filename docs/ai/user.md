# User AI Rules

이 문서는 User lifecycle, public author 표시, profile 정책, user batch 정책을 정리한다.

## 현재 정책 - User Lifecycle

- `User`는 account lifecycle aggregate root다.
- 상태 변경은 service에서 필드를 직접 변경하지 않고 domain method로 수행한다.
- 사용자 상태는 `ACTIVE`, `WITHDRAWN_REQUESTED`, `WITHDRAWN`, `DORMANT`, `SUSPENDED`를 사용한다.
- 일반 회원 탈퇴는 row 삭제가 아니라 status 기반 lifecycle로 처리한다.
- 탈퇴 요청 시 `WITHDRAWN_REQUESTED`가 되고, 유예 기간 내에는 복구 가능하다.
- 탈퇴 완료 시 `WITHDRAWN`이 되며 개인정보를 masking한다.
- 장기 미로그인 사용자는 batch에서 휴면 알림 또는 휴면 전환 대상이 될 수 있다.
- `SUSPENDED` 계정은 login/API 접근 제한 대상이다.
- User lifecycle 정책과 public author 표시 정책은 분리한다.

## 현재 정책 - AuthorSummary

- Post/Comment 응답은 작성자 표시용 `AuthorSummary`를 우선 사용한다.
- `ACTIVE`: `id=userId`, `displayName=nickname`, `status=ACTIVE`, `profileImageUrl=user.profileImageUrl`.
- `SUSPENDED`: `id=userId`, `displayName=차단된 사용자`, `status=SUSPENDED`, `profileImageUrl=null`.
- `WITHDRAWN`: `id=null`, `displayName=탈퇴한 사용자`, `status=WITHDRAWN`, `profileImageUrl=null`.
- `UNKNOWN`: `id=null`, `displayName=알 수 없는 사용자`, `status=UNKNOWN`, `profileImageUrl=null`.
- `SUSPENDED`, `WITHDRAWN`, `UNKNOWN` 작성자는 `profileImageUrl`을 노출하지 않는다.
- `UserClient.findAuthorsByIds(...)` 실패 또는 user row 없음은 조회 자체를 실패시키지 않고 `UNKNOWN` fallback을 사용한다.
- deleted comment는 tombstone 정책이 우선이므로 `author=null`을 유지한다.

## 현재 정책 - User Batch / Dormancy Notify

- `UserBatchWorker`는 batch query 결과를 그대로 믿지 않고 userId로 User를 다시 조회한 뒤 처리한다.
- `DORMANCY_NOTIFY` eventKey는 `OutboxEventKey.dormancyNotify(userId, lastLoginAt.toLocalDate())` 기준이다.
- `UserBatchWorker`는 `OutboxRepository`에 직접 의존하지 않는다.
- 중복 방어는 `OutboxPublisher.publishIfAbsent(...)`에 위임한다.
- `publishIfAbsent(...)`가 `false`를 반환해도 worker는 실패하지 않는다.
- 같은 휴면 사이클의 `DORMANCY_NOTIFY` 중복은 정상적으로 흡수될 수 있다.

## 현재 정책 - ProfileDetail

- `profileImageUrl`은 null을 허용한다.
- 프로필 수정에서 blank 값은 기존 값을 유지한다.
- 내부 업로드 이미지 URL만 허용한다.
- 허용 형식은 `/images/{uuid}.{ext}`이다.
- 허용 확장자는 `jpg`, `jpeg`, `png`, `gif`, `webp`이다.
- 외부 URL, `javascript:`, `data:`, `file:`, protocol-relative URL, `../`, query, fragment, `svg`는 거부한다.

## 주의사항

- `User` entity에서 Repository, Redis, Outbox, MailSender, 외부 API를 직접 호출하지 않는다.
- 탈퇴/마스킹 정책과 public author 표시 정책을 섞지 않는다.
- profile image 노출 정책은 User/AuthorSummary에서 관리하고, Image 문서는 저장/lifecycle 정책만 담당한다.

## TODO

- suspension audit 기록이 필요하면 별도 admin/audit 정책으로 추가한다.
- batch metric, retry/failure report를 보강한다.
- User lifecycle policy가 더 커지면 `WithdrawalPolicy`, `DormancyPolicy`, `SuspensionPolicy` 분리를 검토한다.
