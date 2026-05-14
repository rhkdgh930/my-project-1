# Image AI Rules

이 문서는 이미지 업로드, URL 파싱, attach/sync, cleanup, profile image 저장 정책을 정리한다.

## 현재 정책 - Image Status

- Image 상태는 `PENDING`, `USED`, `DETACHED`, `DELETED`를 사용한다.
- 업로드 직후 이미지는 `PENDING`이다.
- 본문 이미지로 연결되면 `USED`가 된다.
- 본문에서 빠지면 `DETACHED`가 되고 `detachedAt`을 기록한다.
- cleanup 완료 후 `DELETED`가 된다.

## 현재 정책 - Upload Validation

- `POST /api/images`는 인증이 필요하다.
- 응답은 `{ storageKey, url }` 형태다.
- 정적 이미지 접근 URL은 `/images/{storageKey}`이다.
- 저장 파일은 backend upload root 아래에 둔다.
- 허용 확장자는 `jpg`, `jpeg`, `png`, `gif`, `webp`이다.
- `svg`는 허용하지 않는다.
- 확장자, content type, magic byte를 함께 검증한다.
- 5MB 초과 파일은 거부한다.
- path traversal 시도는 거부한다.

## 현재 정책 - ImageUrlParser

- 게시글 본문에서 내부 이미지 URL만 storageKey로 추출한다.
- 내부 URL 형식은 `/images/{uuid}.{ext}`이다.
- 외부 URL, query, fragment, encoded path, 잘못된 내부 URL은 lifecycle 대상에서 제외한다.
- 중복 storageKey는 제거한다.
- 게시글 본문 이미지 lifecycle은 parser 결과를 기준으로 한다.

## 현재 정책 - Attach / Sync

- Post create/update/delete는 Outbox를 통해 이미지 sync를 처리한다.
- `POST_CREATED`, `POST_UPDATED` payload는 post 본문에 포함된 storageKey 목록을 가진다.
- `POST_DELETED`는 빈 목록으로 sync하여 기존 POST 이미지를 detach 대상으로 만든다.
- `attachImages`는 storageKeys를 distinct 처리한다.
- `Image.attach`는 멱등성 및 상태 검증을 담당한다.
- 이미 같은 owner에 붙은 이미지를 다시 처리해도 성공한다.
- 다른 owner에 이미 붙은 이미지는 거부한다.

## 현재 정책 - Cleanup

- `PENDING` cleanup은 오래된 미사용 업로드를 대상으로 한다.
- `DETACHED` cleanup은 `detachedAt` 기준으로 대상을 조회한다.
- 파일 삭제에 성공한 이미지만 `DELETED` 처리한다.
- 파일이 없어도 삭제 성공으로 처리할 수 있다.
- 파일 삭제 실패는 숨기지 않고 cleanup 실패로 기록한다.

## 현재 정책 - Profile Image

- 프로필 이미지는 `POST /api/images` 응답의 `/images/{storageKey}` 값을 `profileImageUrl`로 저장한다.
- `profileImageUrl`은 내부 `/images/{uuid}.{ext}` 형식만 허용한다.
- 외부 URL, query, fragment, `javascript:`, `data:`, `file:`, protocol-relative URL, `../`, `svg`는 거부한다.
- SUSPENDED/WITHDRAWN/UNKNOWN 작성자 profile image 비노출은 `docs/ai/user.md`를 따른다.

## 주의사항

- 게시글 본문 이미지 lifecycle과 profile image cleanup을 강하게 묶지 않는다.
- 외부 `profileImageUrl` 허용 정책을 현재 정책처럼 문서화하지 않는다.

## TODO

- profile image 교체 시 이전 이미지 cleanup 정책을 설계한다.
- 5MB multipart 초과 응답을 명확한 API error shape로 고정한다.
