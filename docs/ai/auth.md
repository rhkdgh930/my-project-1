# Auth AI Rules

이 문서는 인증/인가, JWT, cookie, Redis token, security error response 정책을 정리한다.

## 현재 정책 - 책임 분리

- `AuthTokenResolver`는 bearer access token과 refresh token cookie/header 해석을 담당한다.
- `CookieProperties`는 `app.cookie` 설정을 바인딩한다.
- `CookieManager`는 refresh token cookie 생성/삭제와 cookie 속성을 담당한다.
- `ErrorResponseWriter`는 Security filter/handler 영역의 JSON error response 작성을 담당한다.
- `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`, `JwtLoginFailureHandler`는 `ErrorResponseWriter`를 사용한다.
- `UserAccountPolicy`는 login/API 접근 가능한 계정 상태를 검증한다.
- `RedisUserContextService`는 `CachedUserContext`를 조회한 뒤 `UserAccountPolicy`로 활성 사용자 정책을 검증한다.
- `PasswordEncoder` bean은 `PasswordConfig` 같은 별도 설정에서 제공한다. `SecurityConfig`가 직접 만들지 않는다.

## 현재 정책 - Login / Account

- 일반 로그인은 Spring Security filter 기반 흐름을 유지한다.
- `JwtLoginFilter`는 login request parsing, email normalize, login attempt block check를 담당한다.
- `RedisLoginAttemptService`는 email을 trim/lowercase normalize한 뒤 실패 횟수를 Redis로 관리한다.
- 실패 횟수 초과 시 `TOO_MANY_LOGIN_FAIL` 정책을 따른다.
- login/API 접근 상태 판단은 `UserAccountPolicy`를 기준으로 한다.
- `WITHDRAWN`, `DORMANT`, `SUSPENDED` 계정은 접근 제한 대상이다.
- `WITHDRAWN_REQUESTED`는 복구 가능 여부에 따라 pending/completed 정책을 따른다.

## 현재 정책 - Redis 사용

- Redis에는 refresh token hash, access token blacklist, reissue history, login attempt count를 저장한다.
- Redis에는 email verification code와 verified flag를 저장한다.
- Redis에는 password reset token raw value가 아니라 hash 또는 hash 기반 key를 저장한다.
- Redis에는 `CachedUserContext`를 저장할 수 있다.
- OAuth2 provider state와 refresh token cookie 속성은 이 정책상 Redis 저장 대상이 아니다.

## 현재 정책 - Reissue

- reissue는 refresh token cookie를 우선 사용한다.
- cookie가 없으면 `Refresh-Token` header로 fallback한다.
- cookie/header가 모두 있고 값이 다르면 `INVALID_REFRESH_TOKEN`이다.
- Redis current refresh token 저장소에는 raw refresh token을 저장하지 않고 hash만 저장한다.
- 요청 refresh token hash가 Redis current hash와 일치할 때만 rotation한다.
- rotation과 short-lived reissue history 저장은 Redis Lua script 기반 원자 처리다.
- rotation 실패 후 reissue history가 있으면 cached `TokenResponse`를 반환할 수 있다.
- rotation 실패 후 history가 없으면 refresh token hash를 삭제하고 `INVALID_REFRESH_TOKEN`으로 실패한다.

## 현재 정책 - Logout

- logout의 access token은 optional이다.
- access token이 없거나 만료되었으면 blacklist 처리를 생략할 수 있다.
- 유효한 access token은 남은 TTL 동안 blacklist 처리한다.
- refresh token이 없으면 refresh token hash 삭제를 생략한다.
- refresh token이 있으면 claim 검증 후 userId를 읽고, Redis 저장 hash와 요청 token hash를 비교한다.
- stored hash가 없거나 request hash와 다르면 `INVALID_REFRESH_TOKEN`이다.
- stored hash와 request hash가 일치할 때만 `deleteRefreshTokenHash(userId)`를 호출한다.
- 이 정책은 과거 refresh token으로 현재 refresh token hash를 삭제하는 것을 막기 위한 것이다.

## 현재 정책 - Email / Password Reset

- email verification code는 Redis 기반 만료 값이다.
- 성공 검증 후 원래 code는 삭제되고, 짧은 verified flag를 저장한다.
- raw verification code는 production log에 남기지 않는다.
- password reset token은 one-time credential이다.
- raw reset token은 Redis에 저장하지 않는다.
- password reset confirm은 Redis `GETDEL` 성격의 `getAndDelete`로 token을 원자 소비한다.
- token consume 성공 후 DB password update가 실패해도 token은 자동 복구하지 않는다.

## 현재 정책 - OAuth2

- `CustomOAuth2UserService`는 Google OAuth2 user info 검증을 담당한다.
- Google `providerId`, provider, email은 필수다.
- `email_verified=false`이면 OAuth2 login은 실패한다.
- 기존 일반 가입 계정과 OAuth2 계정이 충돌하면 실패한다.
- OAuth2 login에도 `UserAccountPolicy.validateLoginAllowed`를 적용한다.
- OAuth2 success는 현재 refresh cookie를 발급하고 access token을 query param으로 전달하는 redirect 방식이다.

## 주의사항

- Security handler/filter 응답은 공통 `ExceptionResponse` shape를 유지한다.
- JSON error response는 UTF-8로 작성한다.
- Security slice test는 `PasswordConfig` 등 필요한 config import 누락에 주의한다.

## TODO

- OAuth2 success의 access token query param 방식은 장기적으로 제거한다.
- 운영 cookie 설정은 `secure`, `sameSite`, `domain`, `path`를 환경별로 분리한다.
- Redis Cluster 사용 시 refresh rotation/reissue history Lua key hash slot 정책을 검토한다.
