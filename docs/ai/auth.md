# Auth Domain AI Rules

## 목적

Auth 도메인은 인증과 인가를 담당한다.

현재 주요 책임은 다음과 같다.

- JWT 로그인
- Access Token 검증
- Refresh Token 재발급
- Logout
- OAuth2 login success handling
- Redis 기반 인증 상태 관리
- Login attempt throttling
- Security exception response

이 문서는 현재 Auth 구조를 무조건 고정하기 위한 문서가 아니다.

목적은 보안상 중요한 규칙은 강하게 보호하고, 현재 구조를 기본값으로 삼되, 더 나은 인증 구조가 가능하면 제안할 수 있게 하는 것이다.

---

## 현재 구조의 기본 의도

현재 Auth는 Spring Security Filter 기반이다.

기본 흐름:

1. `JwtLoginFilter`가 로그인 요청을 받는다.
2. `AuthenticationManager`가 인증을 위임받는다.
3. `CustomAuthenticationProvider`가 password와 user 상태를 검증한다.
4. 로그인 성공 시 `JwtLoginSuccessHandler`가 token을 발급한다.
5. Refresh Token hash를 Redis에 저장한다.
6. 요청마다 `JwtAuthenticationFilter`가 Access Token을 검증한다.
7. Redis blacklist와 user context를 확인한다.
8. SecurityContext를 설정한다.

---

## HARD RULES

다음은 반드시 지킨다.

- raw Refresh Token을 Redis에 저장하지 않는다.
- raw Access Token을 Redis blacklist key로 저장하지 않는다.
- Password, JWT, Refresh Token, verification code를 로그에 남기지 않는다.
- 인증 실패 시 SecurityContext를 clear한다.
- Access Token과 Refresh Token의 token type을 검증한다.
- Refresh Token 재발급 시 Redis에 저장된 hash와 요청 token hash를 비교한다.
- Redis에 저장된 refresh token hash가 불일치하면 기존 refresh token state를 제거한다.
- Logout 시 refresh token hash 제거와 access token blacklist 처리를 유지한다.
- suspended, withdrawn, dormant 등 계정 상태를 인증 과정에서 무시하지 않는다.
- 보안상 중요한 Redis 검증 실패를 조용히 통과시키지 않는다.
- Authentication/Authorization error response shape을 임의 변경하지 않는다.
- permitAll 범위를 이유 없이 넓히지 않는다.

---

## DEFAULT RULES

작은 변경에서는 현재 구조를 유지한다.

- 로그인은 Filter 기반 구조를 유지한다.
- 일반 로그인은 `AuthenticationManager`를 통해 처리한다.
- JWT 생성/파싱/검증은 `JwtProvider`가 담당한다.
- 로그인 성공 처리는 success handler에서 수행한다.
- 로그인 실패 처리는 failure handler에서 수행한다.
- Access Token 검증은 `JwtAuthenticationFilter`가 담당한다.
- User 상태 cache는 Redis를 사용할 수 있다.
- DB의 User 상태가 최종 source of truth이다.
- Login attempt 제한은 Redis 기반으로 유지한다.
- User 상태 변경 후 cache/token 무효화는 Outbox와 연계한다.

---

## IMPROVEMENT OPTIONS

다음 개선은 제안 가능하다.

단, 바로 적용하지 말고 tradeoff를 먼저 설명한다.

- Filter 기반 로그인과 Controller 기반 로그인의 비교
- `UserDetailsImpl` 책임 축소
- `CachedUserContext`와 `UserDetailsImpl` mapping 중복 제거
- Auth와 User 도메인의 의존 방향 개선
- Redis user context cache consistency 개선
- Refresh Token rotation 정책 강화
- OAuth2 login success flow 개선
- Cookie secure/sameSite/httpOnly 설정을 환경 설정으로 분리
- SecurityConfig 가독성 개선
- 인증 실패 응답 일관성 개선
- JWT claim 최소화
- TokenService 책임 분리

---

## Filter 기반 로그인에 대한 기준

현재는 Filter 기반 로그인을 기본값으로 한다.

하지만 다음 경우에는 Controller 기반 로그인 또는 다른 구조를 제안할 수 있다.

- 로그인 실패 응답 커스터마이징이 지나치게 복잡해지는 경우
- Security Filter와 application service 책임이 섞이는 경우
- 테스트가 지나치게 어려워지는 경우
- OAuth2, JWT, Redis 흐름의 통합 정책이 더 명확한 구조를 요구하는 경우

제안 시 반드시 비교한다.

- Spring Security integration
- failure handling
- testability
- maintainability
- API consistency
- migration cost

명시적 승인 없이 바로 구조 전환하지 않는다.

---

## Redis 사용 기준

Auth에서 Redis 사용이 적합한 것:

- refresh token hash
- access token blacklist
- reissue deduplication/history
- login attempt count
- email verification code
- verified email flag
- password reset token
- cached user context
- dormancy notification deduplication

Redis에 저장하면 안 되는 것:

- 영구 user role
- 영구 user status
- 영구 account status
- 영구 suspension policy
- 영구 withdrawal state
- password
- raw token

---

## User Context Cache 기준

Redis user context cache는 성능 최적화다.

- cache hit이어도 active user validation을 수행한다.
- cache miss 시 DB에서 조회할 수 있다.
- user 상태 변경 시 필요한 경우 cache evict event를 발행한다.
- token 무효화가 필요한 상태 변경은 refresh token hash 제거를 고려한다.

개선 제안 가능:

- cache TTL 조정
- cache invalidation 이벤트 정리
- DB fallback 정책 개선
- stale cache 허용 범위 명확화

---

## OAuth2 기준

현재 OAuth2 로그인도 JWT 발급 흐름을 사용한다.

기본 규칙:

- OAuth2 로그인도 user lifecycle 정책을 우회하지 않는다.
- OAuth2 로그인 성공 후 `UserLoginService.processLogin` 흐름을 유지한다.
- redirect URL과 cookie 정책은 운영 환경을 고려해야 한다.

개선 제안 가능:

- frontend redirect 설정 분리
- access token query param 제거
- secure cookie 정책 개선
- OAuth2 가입/로그인 정책 분리
- social account linking 정책 추가

---

## Auth 변경 시 검증 기준

Auth 변경 시 다음 검증을 고려한다.

- 정상 로그인 성공
- 로그인 실패 count 증가
- login attempt block
- Access Token 만료
- Refresh Token 만료
- 잘못된 token type
- blacklist token 거부
- refresh token hash mismatch
- logout 후 access token 거부
- suspended user 인증 거부
- withdrawn requested user 인증 거부
- dormant user 인증 거부
- OAuth2 login success flow

---

## 명시적 요청 없이는 하지 말 것

- JWT를 session 기반 인증으로 교체
- Redis 없는 refresh token 구조로 변경
- 인증 실패 응답 shape 변경
- SecurityConfig 전체 rewrite
- OAuth2 흐름 전체 교체
- public auth API path 변경

---

## Auth 구조 변경 기준

Auth 구조 변경은 보안 영향이 크므로, 중간 규모 이상의 구조 변경은 바로 적용하지 않는다.

다음 변경은 반드시 먼저 제안만 해야 한다.

- Filter 기반 로그인에서 Controller 기반 로그인으로 변경
- Refresh Token 저장 전략 변경
- Redis user context cache 제거 또는 대체
- OAuth2 success flow 전체 변경
- SecurityConfig 대규모 재작성

이 경우 Codex는 먼저 다음을 설명해야 한다.

1. 현재 구조의 문제
2. 대안 구조
3. 보안 영향
4. migration plan
5. 필요한 테스트

## Password Reset Token Policy

Password reset token은 one-time credential이다.

- Redis에는 raw token을 저장하지 않는다.
- hash(rawToken) 기반 key만 사용한다.
- resetPassword 시 token은 원자적으로 consume되어야 한다.
- Redis GETDEL 또는 동등한 원자 연산을 사용한다.
- consume 이후 DB 처리 실패가 발생해도 token은 복구하지 않는다.
- 사용자는 password reset을 다시 요청해야 한다.