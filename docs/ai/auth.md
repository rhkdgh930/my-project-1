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

## Refresh Token Reissue Policy

Refresh token reissue는 Redis Lua script 기반 원자 rotation을 사용한다.

기본 원칙:

- Redis의 current refresh token 저장소에는 raw refresh token을 저장하지 않는다.
- `auth::rt::{userId}`에는 refresh token hash만 저장한다.
- 요청 refresh token hash가 Redis의 current refresh token hash와 일치할 때만 새 refresh token hash로 교체한다.
- rotation 성공과 reissue history 저장은 하나의 Redis Lua script 안에서 원자적으로 처리한다.
- rotation 실패 후 reissue history가 있으면 cached response를 반환할 수 있다.
- rotation 실패 후 reissue history가 없으면 invalid refresh token으로 처리한다.

### Reissue History Tradeoff

중복 reissue 요청을 처리하기 위해 짧은 TTL의 reissue history를 저장할 수 있다.

- reissue history는 old refresh token hash를 key로 사용한다.
- value는 cached `TokenResponse`이다.
- 이 cached response에는 새 refresh token이 포함될 수 있다.
- TTL은 매우 짧게 유지한다.
- 이 값은 refresh token state가 아니라 duplicate request response cache이다.
- 운영 환경에서는 Redis 접근 권한과 dump/log 정책을 엄격히 관리한다.

### Redis Cluster 주의사항

현재 Lua script는 current refresh token key와 reissue history key를 함께 접근한다.

Redis Cluster 환경에서는 Lua script가 접근하는 모든 key가 같은 hash slot에 있어야 한다.

- 단일 Redis 또는 Sentinel 환경에서는 문제가 없다.
- Redis Cluster를 사용할 경우 key hash tag 설계를 검토해야 한다.
- Cluster 대응 전에는 이 구조를 그대로 배포하지 않는다.

---

## Current Auth/Security Refactor Policy

이 섹션은 최근 Auth/Security 리팩토링 이후의 현재 구현 기준을 기록한다.
아래 내용은 TODO가 아니라 현재 코드가 따르는 책임 분리와 정책이다.

### Token Resolution

- `AuthTokenResolver`가 auth request token 해석을 담당한다.
- Authorization header의 optional bearer access token을 해석한다.
- refresh token은 cookie 값을 우선 사용하고, 없으면 `Refresh-Token` header로 fallback한다.
- refresh token cookie와 `Refresh-Token` header가 모두 있고 값이 다르면 `INVALID_REFRESH_TOKEN`으로 실패한다.
- required refresh token이 없으면 `INVALID_REFRESH_TOKEN`으로 실패한다.

### Refresh Token Cookie

- `CookieManager`가 refresh token cookie 발급과 삭제를 담당한다.
- `CookieProperties`가 `app.cookie` 설정을 바인딩한다.
- refresh token cookie의 name, secure, httpOnly, sameSite, path, domain은 `CookieProperties` 기준으로 처리한다.
- 로그인 성공, reissue 성공, restore 성공 시 refresh token cookie를 발급한다.
- logout 성공 시 refresh token cookie를 삭제한다.

### Security Error Response

- `ErrorResponseWriter`가 Security handler/filter 영역의 JSON error response 작성을 담당한다.
- `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`, `JwtLoginFailureHandler`는 직접 JSON을 조립하지 않고 `ErrorResponseWriter`를 사용한다.
- 공통 `ExceptionResponse` shape를 유지한다.
- `JwtAuthenticationEntryPoint`는 `JwtAuthenticationException`의 `ErrorCode`를 응답에 반영한다.
- `JwtAccessDeniedHandler`는 `ACCESS_DENIED`를 응답한다.
- `JwtLoginFailureHandler`는 로그인 실패 원인별 `ErrorCode`와 필요한 data를 `ExceptionResponse` shape로 응답한다.

### User Account Policy

- `UserAccountPolicy`가 login/API 접근 가능한 계정 상태 정책을 담당한다.
- 일반 로그인/OAuth2 로그인에서 withdrawn, withdrawal requested, dormant, suspended 상태를 검증한다.
- API 접근 검증에서는 `UserStatus`, `AccountStatus`, deleted 상태를 기준으로 JWT 인증 실패용 `ErrorCode`를 결정한다.
- `RedisUserContextService`는 `CachedUserContext`를 조회하고, active user 검증은 `UserAccountPolicy`로 위임한다.
- Redis user context cache miss 또는 Redis 조회 실패 시 DB fallback을 사용한다.

### Reissue

- reissue는 refresh token cookie를 우선 사용한다.
- refresh token cookie가 없으면 `Refresh-Token` header를 fallback으로 사용한다.
- cookie와 header가 모두 있고 값이 다르면 `INVALID_REFRESH_TOKEN`으로 실패한다.
- refresh token rotation은 Redis에 저장된 refresh token hash와 요청 token hash를 비교하는 기존 정책을 유지한다.
- reissue history fallback을 유지한다.
- active user 검증은 `RedisUserContextService`의 `CachedUserContext` 기반으로 수행한다.

### Logout

- logout의 access token은 optional이다.
- access token이 있으면 access token blacklist를 시도한다.
- expired access token은 blacklist를 생략하고 logout 처리를 계속한다.
- refresh token cookie 또는 `Refresh-Token` header가 있으면 refresh token hash 삭제를 시도한다.
- refresh token이 없어도 logout은 성공한다.
- refresh token cookie와 header가 모두 있고 값이 다르면 `INVALID_REFRESH_TOKEN`으로 실패한다.
- 성공 시 `CookieManager.deleteRefreshTokenCookie`로 refresh token cookie를 삭제한다.

### Login Attempt

- `RedisLoginAttemptService`는 email을 trim + lowercase normalize한 뒤 실패 횟수를 관리한다.
- 빈 email은 실패 횟수 증가와 block check에서 방어적으로 처리한다.
- 로그인 성공 시 normalize된 email 기준으로 실패 횟수를 초기화한다.

### OAuth2

- `CustomOAuth2UserService`는 Google OAuth2 user info 검증을 담당한다.
- Google OAuth2 user info에서 provider id와 email은 필수다.
- `email_verified=false`이면 OAuth2 login을 실패시킨다.
- 기존 계정이 있으면 social login method와 provider id가 일치하는지 검증한다.
- 신규 social user 생성 시 encoded random password를 저장한다.
- OAuth2 login도 `UserAccountPolicy.validateLoginAllowed`로 user lifecycle 정책을 검증한다.
- `OAuth2LoginFailureHandler`는 OAuth2 실패 시 frontend login path로 redirect한다.
- OAuth2 login success는 refresh token cookie를 발급하고, 현재는 access token을 query param으로 포함해 frontend success path로 redirect한다.

### SecurityConfig / PasswordEncoder

- `PasswordEncoder` bean은 `SecurityConfig`가 아니라 `PasswordConfig`에서 제공한다.
- `SecurityConfig`는 `PasswordEncoder` bean을 직접 만들지 않는다.
- `SecurityConfig`는 `PasswordEncoder`를 주입받아 `CustomAuthenticationProvider` 구성에 사용한다.

### TODO

- OAuth2 success redirect의 access token query param 방식은 장기적으로 제거를 검토한다.
- 장기 목표는 refresh token cookie 기반 reissue 흐름으로 access token을 획득하는 구조다.
- 이 TODO는 현재 구현 정책이 아니며, 별도 보안 영향 검토와 migration plan 없이 즉시 변경하지 않는다.
