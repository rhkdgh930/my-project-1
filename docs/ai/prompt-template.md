# Codex Prompt Template

이 파일은 Codex에게 작업을 요청할 때 사용하는 프롬프트 템플릿이다.

규칙 문서는 한글로 유지하되, 실제 Codex 요청은 영어로 작성하는 것을 권장한다.

---
## Mode Definition

이 문서에서 사용하는 Mode는 Codex의 내장 기능이 아니라, 프롬프트에서 사용하는 작업 방식 정의다.

Codex는 사용자가 특정 Mode를 지정하면 아래 규칙에 따라 행동해야 한다.

---

### Architecture Review Mode

이 모드는 코드를 수정하지 않고 구조를 검토할 때 사용한다.

Rules:

- Do NOT modify code.
- Do NOT apply patches.
- Do NOT format files.
- Only analyze and review the current design.
- Challenge the current design instead of assuming it is correct.
- Identify risks, tradeoffs, and alternatives.
- Classify recommendations into:
    - keep as-is
    - small refactor
    - medium refactor
    - major redesign
- Return the answer in Korean if requested.

---

### Surgical Mode

이 모드는 작은 버그 수정, 제한된 리팩토링, 특정 클래스 개선에 사용한다.

Rules:

- Modify only the files required for the task.
- Preserve current behavior unless the task is to fix a confirmed bug.
- Do not perform unrelated cleanup.
- Do not apply medium or major refactors.
- Do not change public API contracts unless explicitly required.
- Prefer the smallest safe change.
- Run or describe relevant verification.

---

### Evolutionary Refactor Mode

이 모드는 현재 구조를 유지하면서 더 나은 구조로 점진적으로 발전시킬 때 사용한다.

Rules:

- Preserve design intent, not necessarily every implementation detail.
- Treat the current architecture as the default, not as an unquestionable truth.
- If a better structure exists, explain it before applying it.
- Do not apply major changes in one step.
- Break large refactors into small safe steps.
- Prefer Step 1 only unless the user explicitly approves more.
- Explain tradeoffs and migration steps before structural changes.

---

## 1. Mode 선택

작업 요청 시 아래 모드 중 하나를 명시한다.

### Surgical Mode

작은 버그 수정, 특정 클래스 리팩토링, 제한된 기능 변경에 사용한다.

- Preserve the current architecture.
- Make the smallest safe change.
- Do not perform unrelated cleanup.
- Do not rewrite code that is not directly related to the task.

### Architecture Review Mode

현재 구조가 좋은지 검토하고 싶을 때 사용한다.

- Do not edit code yet.
- Challenge the current design.
- Identify architectural problems and better alternatives.
- Classify suggestions into:
    - keep as-is
    - small refactor
    - medium refactor
    - major redesign

### Evolutionary Refactor Mode

현재 구조를 유지하되 더 좋은 구조로 점진적으로 발전시키고 싶을 때 사용한다.

- Preserve the current design intent, not necessarily the exact implementation.
- If a better structure exists, propose it first.
- Do not apply major changes without explaining tradeoffs and migration steps.
- Prefer small safe steps over large rewrites.

---

## 2. 기본 요청 템플릿

아래 템플릿을 복사해서 사용한다.

```text
Use <Surgical Mode | Architecture Review Mode | Evolutionary Refactor Mode>.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/<domain>.md
- docs/ai/testing.md

Task:
<Describe the task clearly.>

Constraints:
- Preserve security, data consistency, and failure recovery rules.
- Treat the current architecture as the default, not as an unquestionable truth.
- If a better design exists, explain it before applying it.
- Make surgical changes unless this is Architecture Review Mode.
- Do not change public API contracts unless explicitly required.
- Do not perform unrelated formatting or cleanup.

Before coding, respond with:
1. Assumptions
2. Current design intent
3. Architecture invariants to preserve
4. Potential better alternatives, if any
5. Chosen approach
6. Files likely to change
7. Verification plan

After coding, respond with:
1. Summary of changes
2. Why the architecture is preserved or improved
3. Behavior changes, if any
4. Tests/build commands run
5. Anything not verified
6. Follow-up recommendations, if any
```

---

## 3. Auth 요청 템플릿

```text
Use Evolutionary Refactor Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/auth.md
- docs/ai/outbox.md
- docs/ai/common.md
- docs/ai/testing.md

Task:
<Describe the auth/security/JWT/OAuth2/Redis auth task.>

Constraints:
- Preserve security-critical rules.
- Treat filter-based login as the current default.
- Do not bypass AuthenticationManager without explaining why.
- Keep Refresh Token stored as hash in Redis unless proposing a reviewed alternative.
- Keep Access Token blacklist behavior unless proposing a reviewed alternative.
- Keep user status validation.
- Keep security error response shape stable.
- If a better auth structure exists, explain tradeoffs before applying it.

Before coding, explain:
1. Assumptions
2. Security invariants
3. Current design weaknesses, if any
4. Alternative designs, if any
5. Chosen approach
6. Verification plan
```

---

## 4. User 요청 템플릿

```text
Use Evolutionary Refactor Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/user.md
- docs/ai/outbox.md
- docs/ai/testing.md

Task:
<Describe the user/account lifecycle task.>

Constraints:
- Treat User as the current aggregate root.
- Preserve lifecycle invariants.
- Do not move business rules to controllers.
- Do not directly mutate userStatus/accountStatus/withdrawal/suspension from services unless the task is to introduce a safer domain abstraction.
- Keep Redis out of domain entities.
- Keep Outbox publishing for cache/token invalidation where needed.
- If User is too large or the lifecycle model can be improved, explain the alternative first.

Before coding, explain:
1. Assumptions
2. User lifecycle invariants
3. Current design weaknesses, if any
4. Alternative designs, if any
5. Outbox events affected
6. Verification plan
```

---

## 5. Outbox 요청 템플릿

```text
Use Evolutionary Refactor Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/outbox.md
- docs/ai/common.md
- docs/ai/testing.md

Task:
<Describe the outbox processing/handler/retry/recovery task.>

Constraints:
- Preserve reliable side-effect handling.
- Treat persistent Outbox as the current default.
- Keep claim-based processing unless proposing a reviewed alternative.
- Keep retry/dead/recovery behavior.
- Do not replace Outbox with direct async calls without comparing reliability, transaction boundary, retry, and idempotency.
- If a better event processing design exists, explain tradeoffs before applying it.

Before coding, explain:
1. Assumptions
2. Outbox state machine invariants
3. Transaction boundaries to preserve
4. Current design weaknesses, if any
5. Alternative designs, if any
6. Verification plan
```

---

## 6. Common 요청 템플릿

```text
Use Surgical Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/common.md
- docs/ai/testing.md

Task:
<Describe the common exception/logging/config/serialization task.>

Constraints:
- Keep error response shape stable unless explicitly required.
- Keep ErrorCode-based business exceptions.
- Keep MDC trace behavior.
- Do not introduce domain-specific logic into common.
- Do not log sensitive information.
- If a better common infrastructure design exists, explain it first.

Before coding, explain:
1. Assumptions
2. Shared contracts to preserve
3. Risk of API/logging behavior changes
4. Verification plan
```

---

## 7. 리뷰 전용 템플릿

```text
Use Architecture Review Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/<domain>.md
- docs/ai/testing.md

Task:
Review the following code for correctness, transaction safety, security risks, architecture violations, and unnecessary complexity.

Do not edit code yet.

Return:
1. What is good and should be kept
2. Critical issues
3. Important but non-critical issues
4. Optional improvements
5. Over-engineered parts, if any
6. Under-designed parts, if any
7. Suggested refactoring roadmap:
   - small refactor
   - medium refactor
   - major redesign
8. What should not be changed
```

---

## 8. 리팩토링 전용 템플릿

```text
Use Evolutionary Refactor Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/<domain>.md
- docs/ai/testing.md

Task:
Refactor <target> to improve <goal>.

Constraints:
- Preserve behavior unless explicitly stated.
- Preserve design intent, not necessarily every implementation detail.
- Keep changes minimal for the first step.
- If a larger redesign is better, propose it separately before applying it.
- Do not change public API contracts unless explicitly required.
- Add or update tests if behavior is affected.

Before coding:
- Identify the smallest safe change.
- Identify whether a better medium/major design exists.
- Explain why the chosen step is safe.
- Explain verification plan.
```

---

## 9. 버그 수정 전용 템플릿

```text
Use Surgical Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/<domain>.md
- docs/ai/testing.md

Task:
Fix <bug description>.

Observed behavior:
<What currently happens.>

Expected behavior:
<What should happen.>

Constraints:
- First identify the root cause.
- Prefer writing or updating a regression test.
- Make the smallest safe fix.
- Do not refactor unrelated code.
- Preserve public API contracts unless the bug is the contract itself.

Before coding:
1. Explain the suspected root cause.
2. Identify the smallest safe fix.
3. Explain the regression test or verification plan.

After coding:
1. Summarize the fix.
2. Explain why the bug is fixed.
3. List tests/build commands run.
4. Mention anything not verified.
```

---

## 10. 테스트 추가 전용 템플릿

```text
Use Surgical Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/<domain>.md
- docs/ai/testing.md

Task:
Add tests for <target behavior>.

Constraints:
- Do not change production behavior unless a bug is discovered.
- Keep tests focused.
- Prefer domain/service tests before broad end-to-end tests.
- If the current design makes testing hard, explain the design issue first.
- Do not weaken existing tests.

Before coding:
1. Identify the behavior to lock down.
2. Identify the best test level.
3. Explain test cases to add.

After coding:
1. Summarize tests added.
2. Explain what behavior is now protected.
3. List test commands run.
4. Mention anything not verified.
```

---

## 11. 사용 예시

### Auth 예시

```text
Use Evolutionary Refactor Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/auth.md
- docs/ai/outbox.md
- docs/ai/common.md
- docs/ai/testing.md

Task:
Review and refactor JwtLoginFailureHandler for null-safety and response consistency.

Constraints:
- Keep filter-based login.
- Keep Redis login attempt policy.
- Do not change the error response shape.
- Make the smallest safe change first.
```

### User 예시

```text
Use Architecture Review Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/user.md
- docs/ai/outbox.md
- docs/ai/testing.md

Task:
Review the current withdrawal, dormancy, and suspension lifecycle design.

Do not edit code yet.

Return:
1. What is good and should be kept
2. Critical issues
3. Medium refactor suggestions
4. Major redesign suggestions, if any
5. Recommended first safe step
```

### Outbox 예시

```text
Use Evolutionary Refactor Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/outbox.md
- docs/ai/testing.md

Task:
Review OutboxProcessor and OutboxEventManager for transaction safety.

Constraints:
- Keep reliable side-effect handling.
- Keep claim-based processing unless a better alternative is explained first.
- Do not remove retry/dead/recovery behavior.
- Propose tests for rollback-only and handler failure cases.
```

### 전체적인 리팩토링 예시

#### 아키텍쳐 구조 검증

```text
Use Architecture Review Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/auth.md
- docs/ai/user.md
- docs/ai/outbox.md
- docs/ai/common.md
- docs/ai/testing.md

Task:
Review the current auth, user, outbox, and common domains for correctness, security, transaction safety, maintainability, and unnecessary complexity.

Do not edit code yet.

Focus on:
- Spring Security + JWT + Redis authentication flow
- User lifecycle: signup, password change, withdrawal, restore, dormancy, suspension
- Outbox processing: claim, retry, dead-letter, recovery, handler behavior
- Redis usage boundaries
- DDD aggregate boundaries
- Transaction boundaries
- Error handling and logging consistency

Return:
1. What is good and should be kept
2. Critical issues that can cause bugs, security risks, or data inconsistency
3. Important but non-critical issues
4. Over-engineered parts, if any
5. Under-designed parts, if any
6. Suggested refactoring roadmap:
   - small refactor
   - medium refactor
   - major redesign
7. Recommended first safe refactoring step
8. Files that should not be changed yet
```

#### 제한된 기능 변경

```text
Use Surgical Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/<related-domain>.md
- docs/ai/testing.md

Task:
Apply only the first recommended small refactoring step from your previous review.

Constraints:
- Do not apply medium or major refactors yet.
- Do not change public API contracts.
- Do not perform unrelated formatting.
- Preserve current behavior unless the issue is a confirmed bug.
- Add or update tests if behavior is affected.

Before coding:
1. Restate the exact issue being fixed
2. List files to change
3. Explain the smallest safe fix
4. Explain verification plan

After coding:
1. Summary of changes
2. Why this is safe
3. Tests/build commands run
4. Anything not verified
```

####구조 점진적 변경

```text
Use Evolutionary Refactor Mode.

Read these files first:

- CODEX.md
- docs/ai/project.md
- docs/ai/auth.md
- docs/ai/user.md
- docs/ai/outbox.md
- docs/ai/common.md
- docs/ai/testing.md

Task:
Refactor the auth, user, outbox, and common domains to improve correctness, maintainability, and testability.

Important:
Do not rewrite the whole project at once.

First, inspect the code and propose a step-by-step refactoring plan.
Then apply only Step 1.

Constraints:
- Preserve security, data consistency, and failure recovery rules.
- Treat the current architecture as the default, not as an unquestionable truth.
- If a better design exists, explain it before applying it.
- Do not change public API contracts unless explicitly required.
- Do not perform unrelated formatting or cleanup.
- Prefer small safe changes.
- Add or update tests if behavior is affected.

Before coding:
1. Assumptions
2. Current design intent
3. Critical risks found
4. Refactoring roadmap
5. Step 1 scope only
6. Files likely to change in Step 1
7. Verification plan

After coding:
1. Summary of Step 1 changes
2. Why the architecture is preserved or improved
3. Behavior changes, if any
4. Tests/build commands run
5. Remaining recommended steps
```