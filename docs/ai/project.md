# Project AI Rules

이 문서는 프로젝트 전체 아키텍처 지도다. 세부 정책은 각 도메인 문서를 우선한다.

## 역할

- 전체 구조와 변경 원칙을 설명한다.
- 도메인별 상세 정책은 링크 수준으로만 요약한다.
- 구현 세부, 테스트 세부, 운영 TODO는 각 문서에 둔다.

## 아키텍처 개요

- Backend는 Spring Boot 기반 layered architecture를 사용한다.
- Controller는 HTTP 입출력과 인증 principal 해석에 집중한다.
- Application Service는 orchestration을 담당한다.
- Domain Entity/Value Object는 핵심 invariant와 상태 전이를 담당한다.
- Repository는 DB 접근을 담당한다.
- Redis는 token/cache/count 같은 임시 상태 저장소로 사용한다.
- DB는 User lifecycle, Board/Post/Comment/Image/Outbox의 source of truth다.
- Outbox는 transaction 이후 재시도 가능한 side effect를 처리한다.
- Common은 예외 응답, logging, clock, serializer 같은 공통 인프라만 담당한다.

## 현재 주요 도메인

- Auth/Security: `docs/ai/auth.md`
- User lifecycle/batch/author policy: `docs/ai/user.md`
- Board/Post/Redis count: `docs/ai/post.md`
- Comment tombstone: `docs/ai/comment.md`
- Image upload/attach/cleanup: `docs/ai/image.md`
- Outbox state/retry/recovery: `docs/ai/outbox.md`
- Common exception/logging/config: `docs/ai/common.md`
- Test strategy: `docs/ai/testing.md`
- Operations TODO: `docs/ai/operations.md`

## Hard Rules

- API response shape를 임의로 변경하지 않는다.
- password, raw token, verification code, password reset token을 로그에 남기지 않는다.
- Controller에 도메인 상태 전이 규칙을 넣지 않는다.
- Entity에서 Repository, Redis, MailSender, 외부 API를 직접 호출하지 않는다.
- 실패한 side effect를 성공으로 기록하지 않는다.
- Outbox claim/retry/recovery 구조를 단순화한다는 이유로 제거하지 않는다.
- SecurityConfig 전체 rewrite는 명시 요청 없이는 하지 않는다.
- `spring.main.allow-circular-references=true`를 사용하지 않는다.
- 테스트를 통과시키기 위해 실제 정책을 약화하지 않는다.
- unrelated cleanup을 하지 않는다.

## Change Policy

작업 전에는 다음을 판단한다.

1. 현재 구조가 의도하는 책임 분리
2. 보존해야 하는 API/security/data consistency invariant
3. 더 나은 대안이 있다면 tradeoff와 migration cost
4. production code 수정이 필요한지, 테스트만 수정하면 되는지
5. 검증 명령

작업 후에는 다음을 남긴다.

1. 변경 요약
2. production code 수정 여부
3. 테스트/빌드 실행 결과
4. 미검증 항목
5. 운영 전 TODO가 생겼다면 `operations.md` 반영 여부
