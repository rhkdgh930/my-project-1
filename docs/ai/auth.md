# Auth / Security AI Rules

이 문서는 Auth, OAuth2, JWT, cookie, security error response 정책을 정의한다.

## 현재 정책

### 책임 분리

- `AuthTokenResolver`는 Authorization bearer token과 refresh token cookie/header 해석을 담당한다.
- refresh token은 cookie 값을 우선 사용하고, 없으면 `Refresh-Token` header로 fallback한다.
- refresh token cookie와 header가 모두 있고 값이 다르면 `INVALID_REFRESH_TOKEN`이다.
- `CookieManager`는 refresh token cookie 생성/삭제를 담당한다.
- `CookieProperties`는 `app.cookie` 설정을 바인딩한다.
- `ErrorResponseWriter`는 security handler/filter 영역의 JSON error response 작성을 담당한다.
- `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`, `JwtLoginFailureHandler`는 `ErrorResponseWriter`를 사용한다.
- `UserAccountPolicy`는 login/API 접근 가능한 계정 상태 정책을 담당한다.
- `RedisUserContextService`는 `CachedUserContext` 조회 후 `UserAccountPolicy`로 active user 검증을 위임한다.
- `PasswordEncoder` bean은 `SecurityConfig` 밖의 설정 클래스에서 제공한다.
- `SecurityConfig`는 `PasswordEncoder` bean을 직접 만들지 않는다.

### Login

- 일반 로그인은 Spring Security filter 기반 흐름을 유지한다.
- `JwtLoginFilter`는 login request parsing, email normalize, login attempt block check를 담당한다.
- `RedisLoginAttemptService`는 email을 trim/lowercase normalize한 뒤 실패 횟수를 관리한다.
- 성공 시 login failure count를 초기화한다.
- 실패 횟수 초과 시 `TOO_MANY_LOGIN_FAIL` 정책을 따른다.

### Account Status

- login/API 접근 상태 판단은 `UserAccountPolicy`를 기준으로 한다.
- WITHDRAWN은 로그인 불가다.
- WITHDRAWN_REQUESTED는 복구 가능 여부에 따라 pending/completed 정책을 따른다.
- DORMANT는 로그인/API 접근 제한 대상이다.
- SUSPENDED는 로그인/API 접근 제한 대상이다.
- API 접근 검증은 cached context가 있으면 cache를 사용하고, cache miss 또는 Redis 실패 시 DB fallback을 사용할 수 있다.

### Auth Redis Usage

- refresh token hash
- access token blacklist
- reissue history fallback
- login attempt count
- email verification code and verified flag
- password reset token hash key
- `CachedUserContext`
- OAuth2 provider state and refresh token cookie attributes are not stored in Redis by this policy.

### Email Verification Code Policy

- email verification code is a Redis-based expiring value.
- raw verification code must not be written to production logs.
- successful verification stores a short-lived verified flag and deletes the original code.
- the same code must not be reusable after successful verification.
- expired or missing code uses `EXPIRED_VERIFICATION_CODE`.
- mismatched code uses `WRONG_VERIFICATION_CODE`.
- developer convenience APIs that expose verification codes must not exist in production.

### Password Reset Token Policy

- password reset token is a one-time credential.
- raw reset token must not be stored in Redis.
- Redis stores a token hash, or a key derived from the token hash.
- password reset confirmation consumes the token atomically.
- current implementation uses Redis `GETDEL` semantics through `getAndDelete`.
- password change proceeds only after token consume succeeds.
- an already consumed token cannot be reused.
- expired, missing, or mismatched reset token fails as a password reset token error.
- current implementation uses `INVALID_EMAIL_TOKEN` for an invalid password reset token.
- if token consume succeeds but DB password update fails, the token is not automatically restored.
- this policy reduces reset token replay risk and limits damage from token leakage.

### Reissue

- reissue는 refresh token cookie를 우선 사용한다.
- cookie가 없으면 `Refresh-Token` header를 fallback으로 사용한다.
- cookie/header가 모두 있고 값이 다르면 `INVALID_REFRESH_TOKEN`이다.
- Redis current refresh token 저장소에는 raw refresh token을 저장하지 않고 hash만 저장한다.
- 요청 refresh token hash가 Redis current hash와 일치할 때만 rotation한다.
- rotation과 short-lived reissue history 저장은 Redis Lua script 기반 원자 처리다.
- rotation 실패 후 reissue history가 있으면 cached `TokenResponse`를 반환할 수 있다.
- rotation 실패 후 history가 없으면 refresh token hash를 삭제하고 `INVALID_REFRESH_TOKEN`으로 실패한다.

### Logout

- logout의 access token은 optional이다.
- access token이 없으면 blacklist 처리를 생략한다.
- expired access token은 blacklist에 등록하지 않고 logout 처리를 계속한다.
- valid access token은 남은 TTL 동안 blacklist에 등록한다.
- refresh token이 없으면 refresh token hash 삭제를 생략한다.
- refresh token이 있으면 claim 검증 후 userId를 읽는다.
- Redis에 저장된 현재 refresh token hash와 요청 refresh token hash를 비교한다.
- stored hash가 없거나 request hash와 다르면 `INVALID_REFRESH_TOKEN`이다.
- stored hash와 request hash가 일치할 때만 `deleteRefreshTokenHash(userId)`를 호출한다.
- 이 정책은 과거 refresh token으로 현재 refresh token hash를 삭제하는 것을 막기 위한 것이다.
- logout 성공 시 `CookieManager.deleteRefreshTokenCookie`로 refresh token cookie를 삭제한다.

### OAuth2

- `CustomOAuth2UserService`는 Google OAuth2 user info 검증을 담당한다.
- Google `providerId`, provider, email은 필수다.
- `email_verified=false`이면 OAuth2 login은 실패한다.
- 기존 일반 가입 계정과 OAuth2 계정이 충돌하면 실패한다.
- 기존 social account의 provider/providerId가 다르면 실패한다.
- 신규 OAuth2 user 생성 시 encoded random password를 저장한다.
- OAuth2 login에도 `UserAccountPolicy.validateLoginAllowed`를 적용한다.
- `OAuth2LoginFailureHandler`는 실패 시 frontend login path로 redirect한다.
- OAuth2 success는 현재 refresh cookie를 발급하고 access token을 query param으로 전달하는 redirect 방식이다.

### Security Error Response

- Security handler/filter 응답은 공통 `ExceptionResponse` shape를 유지한다.
- `JwtAuthenticationEntryPoint`는 `JwtAuthenticationException`의 `ErrorCode`를 반영한다.
- `JwtAccessDeniedHandler`는 `ACCESS_DENIED`를 반환한다.
- `JwtLoginFailureHandler`는 로그인 실패 원인별 `ErrorCode`와 필요한 data를 반환한다.
- JSON 응답은 UTF-8로 작성한다.

## 테스트 기준

- login success/failure/count/block/email normalize를 검증한다.
- account status별 login 실패 정책을 검증한다.
- reissue cookie/header fallback, mismatch, rotation/history fallback을 검증한다.
- logout optional access token, expired access token, refresh token hash 검증을 검증한다.
- CookieManager/CookieProperties 기반 Set-Cookie/delete cookie 정책을 검증한다.
- OAuth2 user info 검증, 기존 계정 충돌, 신규 social user 생성, failure/success handler를 검증한다.
- Security slice test는 `PasswordConfig` 등 필요한 config import 누락에 주의한다.

## TODO / 운영 전 개선

- OAuth2 success의 access token query param 방식은 장기적으로 제거한다.
- 목표 구조는 refresh token HttpOnly cookie 기반 reissue로 access token을 획득하는 방식이다.
- 운영 cookie 설정은 `secure=true`, 적절한 `sameSite`, `domain`, `path`를 환경별로 분리한다.
- Redis Cluster 사용 시 refresh rotation/reissue history Lua key hash slot 정책을 검토한다.
- secret/env/profile 정리는 `docs/ai/operations.md`를 따른다.
