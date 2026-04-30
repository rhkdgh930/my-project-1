# User Domain AI Rules

## 목적

User 도메인은 사용자 계정의 핵심 lifecycle을 담당한다.

현재 주요 책임은 다음과 같다.

- 회원가입
- 소셜 회원가입
- 이메일 인증 orchestration
- 프로필 수정
- 비밀번호 변경
- 비밀번호 재설정
- 회원 탈퇴 요청
- 탈퇴 복구
- 탈퇴 완료
- 휴면 전환
- 휴면 복구
- 관리자 차단/차단 해제
- User batch 처리

이 문서는 User 구조를 무조건 고정하기 위한 것이 아니다.

목적은 User lifecycle의 정합성을 보호하면서, 현재 구조를 출발점으로 더 나은 구조로 점진적으로 발전시키는 것이다.

---

## 현재 구조의 기본 의도

현재 `User`는 account lifecycle의 aggregate root이다.

User 상태 변경은 service에서 직접 필드를 변경하지 않고 domain method를 통해 수행한다.

주요 domain method:

- `signUp`
- `socialSignUp`
- `updateProfile`
- `updateNickname`
- `updatePassword`
- `requestWithdrawal`
- `cancelWithdrawal`
- `completeWithdrawal`
- `recordSuccessfulLogin`
- `markDormant`
- `suspend`
- `unSuspend`
- `checkAndReleaseSuspension`

---

## HARD RULES

다음은 반드시 지킨다.

- Controller에 User lifecycle 로직을 넣지 않는다.
- Domain Entity에서 Repository를 호출하지 않는다.
- Domain Entity에서 Redis를 호출하지 않는다.
- Domain Entity에서 Outbox를 직접 발행하지 않는다.
- Domain Entity에서 MailSender나 외부 API를 호출하지 않는다.
- Password 검증이 필요한 flow에서 검증을 제거하지 않는다.
- 비밀번호, reset token, verification code를 로그에 남기지 않는다.
- 탈퇴/차단/휴면 상태 검증을 조용히 우회하지 않는다.
- User 상태 변경 후 cache/token 영향이 있는데 이를 무시하지 않는다.
- 개인정보 masking 정책을 근거 없이 제거하지 않는다.
- Soft delete 정책을 명시적 요청 없이 제거하지 않는다.

---

## DEFAULT RULES

작은 변경에서는 현재 구조를 유지한다.

- `User`를 account lifecycle aggregate root로 본다.
- User 상태 전이는 domain method를 통해 수행한다.
- `UserCommandService`는 orchestration을 담당한다.
- `UserQueryService`는 조회를 담당한다.
- Admin command/query 분리를 유지한다.
- User batch는 chunk 기반으로 처리한다.
- User 상태 변경 후 필요한 경우 Outbox를 발행한다.
- Redis는 user lifecycle의 source of truth가 아니다.
- 시간 의존 로직은 Clock을 사용한다.
- 비즈니스 예외는 `CustomException(ErrorCode.X)`를 사용한다.

---

## IMPROVEMENT OPTIONS

다음 개선은 제안 가능하다.

단, 바로 적용하지 말고 tradeoff를 먼저 설명한다.

- User aggregate가 과도하게 커질 경우 정책 객체로 분리
    - `UserLifecyclePolicy`
    - `WithdrawalPolicy`
    - `SuspensionPolicy`
    - `DormancyPolicy`
- 일부 상태 전이 로직을 Domain Service로 분리
- UserCommandService orchestration 단순화
- User account lifecycle event 발행 정책 정리
- `UserAccountChangedType` 명명과 token/cache flag 재검토
- Withdrawal/Dormancy/Suspension을 더 명시적인 model로 개선
- soft delete와 withdrawal masking 정책 분리
- batch transaction 단위 개선
- user lookup 정책 개선
- Auth와 User의 결합도 낮추기
- DTO 변환 책임 정리

---

## User Aggregate 분리 기준

현재는 User aggregate 중심 구조를 기본값으로 한다.

하지만 다음 신호가 보이면 분리를 제안할 수 있다.

- `User`가 너무 많은 정책을 직접 가진다.
- withdrawal, dormancy, suspension 정책이 계속 복잡해진다.
- 테스트가 어려워진다.
- 상태 전이 규칙이 여러 도메인과 강하게 얽힌다.
- Auth 요구사항 때문에 User entity가 보안 adapter처럼 변한다.

분리 제안 시 주의한다.

- Entity를 빈약한 data holder로 만들지 않는다.
- Service에 상태 변경 로직을 무질서하게 흩뿌리지 않는다.
- 정책 객체나 domain service로 분리하더라도 invariant의 소유자를 명확히 한다.
- migration은 작은 단계로 제안한다.

---

## User Status 기준

`UserStatus` 의미:

- `ACTIVE`: 정상 사용자
- `WITHDRAWN_REQUESTED`: 탈퇴 요청 후 유예 기간 중
- `WITHDRAWN`: 탈퇴 완료
- `DORMANT`: 휴면 사용자

상태 전이 규칙은 기본적으로 User domain method 안에 둔다.

대안 구조를 제안할 경우 상태 전이 invariant가 어디에서 보장되는지 명확히 설명해야 한다.

---

## Account Status 기준

`AccountStatus` 의미:

- `NORMAL`: 정상 계정
- `SUSPENDED`: 차단 계정

차단 정책은 현재 `UserSuspension`이 담당한다.

개선 제안 가능:

- temporary/permanent suspension 정책 명확화
- suspension merge/extend 규칙 테스트 추가
- expired suspension release 정책 개선
- admin suspension audit 기록 추가

---

## Withdrawal 기준

현재 정책:

- 탈퇴 요청 후 즉시 물리 삭제하지 않는다.
- 유예 기간 동안 복구 가능하다.
- 유예 기간이 지나면 batch로 탈퇴 완료 처리한다.
- 탈퇴 완료 시 개인정보를 masking한다.
- 탈퇴 완료 계정은 인증/조회에서 일반 사용자처럼 취급하지 않는다.

개선 제안 가능:

- withdrawal requested 상태에서 허용되는 action 명확화
- soft delete와 masking의 책임 분리
- 복구 가능 기간 계산 방식 개선
- batch cleanup 기준 테스트 추가

---

## Dormancy 기준

현재 정책:

- 장기 미로그인 유저는 휴면 전환 대상이다.
- 휴면 전환 전 notification을 발송할 수 있다.
- 휴면 전환 시 cache/token 무효화를 고려한다.
- 로그인 성공 시 휴면 해제될 수 있다.

개선 제안 가능:

- notification deduplication 정책 개선
- dormant 전환 기준 상수화/설정화
- dormant release flow 명확화
- batch query와 transaction 단위 개선

---

## Outbox 연동 기준

다음 변경은 `USER_ACCOUNT_CHANGED` 이벤트 발행을 검토한다.

- 프로필 변경
- 휴면 전환
- 휴면 해제
- 탈퇴 요청
- 탈퇴 복구
- 탈퇴 완료
- 비밀번호 변경
- 비밀번호 재설정
- 관리자 차단
- 관리자 차단 해제

`UserAccountChangedType`의 의미:

- `shouldEvictCache`: Redis user context cache 제거 필요
- `shouldInvalidateToken`: Refresh Token 제거 필요

개선 제안 가능:

- type 이름이 실제 의미와 맞는지 검토
- token invalidation과 cache evict를 별도 event로 분리
- audit event와 auth invalidation event 분리
- Outbox event key 정책 개선

---

## Batch 기준

현재 batch는 chunk 기반 처리를 기본값으로 한다.

기본 규칙:

- 전체 유저를 한 번에 로딩하지 않는다.
- cursor/id 기반으로 chunk 처리한다.
- 한 유저 실패가 전체 job 실패로 이어지지 않도록 한다.
- 실패 시 userId와 함께 log를 남긴다.
- 상태 변경은 domain method를 사용한다.
- 필요한 Outbox event를 발행한다.

개선 제안 가능:

- transaction per user vs transaction per chunk 비교
- entity loading 대신 id projection 후 단건 처리
- retry/failure report 개선
- batch 실행 중 중복 실행 방지
- batch metric 추가

---

## User 변경 시 검증 기준

User 변경 시 다음 테스트를 고려한다.

- signup
- duplicated email
- email verification required
- password update current password mismatch
- same password rejection
- withdrawal request
- withdrawal restore within retention period
- withdrawal restore after retention period
- withdrawal completion masking
- dormant transition
- dormant release on login
- temporary suspension
- permanent suspension
- suspension release
- user account changed outbox event publishing

---

## 명시적 요청 없이는 하지 말 것

- User aggregate를 단순 data holder로 변경
- lifecycle 규칙을 Controller로 이동
- password 검증 제거
- withdrawal retention 제거
- dormancy 정책 제거
- soft delete 제거
- User service 전체 rewrite
- Auth 도메인 내부로 User 도메인 흡수

## User Deletion Policy

User의 일반 회원 탈퇴는 JPA delete로 처리하지 않는다.

- 탈퇴 요청: `UserStatus.WITHDRAWN_REQUESTED`
- 탈퇴 확정: `UserStatus.WITHDRAWN` + 개인정보 비가역 마스킹
- 탈퇴 확정 시 `deletedAt`은 세팅하지 않는다.
- 탈퇴 완료 유저 row는 게시글/댓글 작성자 표시, 감사, 재가입 정책 처리를 위해 유지한다.
- `userRepository.delete(user)`는 일반 탈퇴 플로우에서 사용하지 않는다.
- `deletedAt`은 운영상 강제 숨김 또는 특수 삭제 용도다.

탈퇴 완료 유저의 기존 게시글/댓글은 자동 삭제하지 않는다.
일반 사용자에게는 작성자를 “탈퇴한 사용자”로 표시한다.