# User Domain AI Rules

이 문서는 User lifecycle, public author 표시, user batch 정책을 정의한다.

## 현재 정책

### User Aggregate

- `User`는 account lifecycle aggregate root다.
- 상태 변경은 service에서 필드를 직접 변경하지 않고 domain method로 수행한다.
- 주요 상태 전이:
  - signup
  - social signup
  - profile update
  - password update
  - withdrawal request/cancel/complete
  - successful login 기록
  - dormancy transition/release
  - suspension/unsuspension
- `User` entity에서 Repository, Redis, Outbox, MailSender, 외부 API를 직접 호출하지 않는다.

### UserStatus

- `ACTIVE`: 정상 사용자
- `WITHDRAWN_REQUESTED`: 탈퇴 요청 후 유예 기간 중인 사용자
- `WITHDRAWN`: 탈퇴 완료 사용자
- `DORMANT`: 휴면 사용자

### AccountStatus

- `NORMAL`: 정상 계정
- `SUSPENDED`: 차단 계정

### Withdrawal / Masking

- 일반 회원 탈퇴는 row 삭제가 아니라 status 기반 lifecycle로 처리한다.
- 탈퇴 요청 시 `WITHDRAWN_REQUESTED`가 된다.
- 유예 기간 내에는 복구 가능하다.
- 탈퇴 완료 시 `WITHDRAWN`이 되며 개인정보를 masking한다.
- 탈퇴 완료 user row는 게시글/댓글 작성자 표시, 감사, 참조 보존을 위해 유지한다.
- `deletedAt`은 일반 회원 탈퇴의 기본 조회 필터로 사용하지 않는다.

### Dormancy

- 장기 미로그인 사용자는 batch에서 휴면 전환 또는 휴면 알림 대상이 될 수 있다.
- 휴면 전환 시 user account changed outbox event를 발행할 수 있다.
- login 성공 시 휴면 해제 정책을 따른다.

### Suspension

- `SUSPENDED` 계정은 login/API 접근 제한 대상이다.
- suspension 만료 해제는 domain policy와 service orchestration을 통해 처리한다.

### AuthorSummary

- Post/Comment 응답은 작성자 표시용 `AuthorSummary`를 우선 사용한다.
- `AuthorSummary`는 `id`, `displayName`, `status`를 가진다.
- `ACTIVE`: `id=userId`, `displayName=nickname`, `status=ACTIVE`
- `WITHDRAWN`: `id=null`, 탈퇴 사용자 표시명, `status=WITHDRAWN`
- `SUSPENDED`: `id=userId`, 차단 사용자 표시명, `status=SUSPENDED`
- `UNKNOWN`: `id=null`, 알 수 없는 사용자 표시명, `status=UNKNOWN`
- `UserClient.findAuthorsByIds(...)` 실패 또는 user row 없음은 조회 자체를 실패시키지 않고 `UNKNOWN` fallback을 사용한다.
- deleted comment는 tombstone 정책을 우선하며 `author=null`을 유지한다.
- 탈퇴/마스킹 정책과 public author 표시 정책은 구분한다.

### User Batch / Dormancy Notify

- `UserBatchWorker`는 batch query 결과를 그대로 믿지 않고 userId로 User를 다시 조회한 뒤 처리한다.
- `DORMANCY_NOTIFY` eventKey는 `OutboxEventKey.dormancyNotify(userId, lastLoginAt.toLocalDate())` 기준이다.
- `UserBatchWorker`는 `OutboxRepository`에 직접 의존하지 않는다.
- 중복 방어는 `OutboxPublisher.publishIfAbsent(...)`에 위임한다.
- `publishIfAbsent(...)`가 `false`를 반환해도 worker는 실패하지 않는다.
- `lastLoginAt`이 null이면 휴면 알림과 휴면 전환 처리 대상이 아니다.

## 테스트 기준

- signup, duplicated email, email verification required를 검증한다.
- password update는 current password mismatch와 same password rejection을 검증한다.
- withdrawal request/restore/complete/masking을 검증한다.
- dormant transition/release를 검증한다.
- temporary/permanent suspension 정책을 검증한다.
- AuthorSummary fallback과 `UserClient` 실패 시 `UNKNOWN` 처리를 검증한다.
- `UserBatchWorkerTest`는 `DORMANCY_NOTIFY` 발행 시 `publishIfAbsent` 사용을 검증한다.

## TODO / 운영 전 개선

- suspension audit 기록이 필요하면 별도 admin/audit 정책으로 추가한다.
- batch metric, retry/failure report를 보강한다.
- User lifecycle policy가 더 커지면 `WithdrawalPolicy`, `DormancyPolicy`, `SuspensionPolicy` 분리를 검토한다.
