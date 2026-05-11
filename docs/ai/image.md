# Image Domain AI Rules

이 문서는 image upload, URL parsing, attach/sync, cleanup 정책을 정의한다.

## 현재 정책

### Image Status

- `PENDING`: 업로드됐지만 아직 owner에 연결되지 않은 상태
- `USED`: owner에 연결된 상태
- `DETACHED`: 더 이상 owner가 사용하지 않는 상태
- `DELETED`: 실제 파일 삭제 성공 후 DB에서도 삭제 상태로 표시된 상태

### Upload / Storage Security

- Image upload API는 authenticated API다.
- 저장 파일명은 서버가 생성한 UUID와 허용 확장자만 사용한다.
- 원본 파일명은 확장자 추출 용도로만 사용하고 저장 경로에 직접 반영하지 않는다.
- 허용 확장자는 `jpg`, `jpeg`, `png`, `gif`, `webp`다.
- SVG는 허용하지 않는다.
- 허용 content type은 `image/jpeg`, `image/png`, `image/gif`, `image/webp`다.
- 확장자, contentType, magic byte를 함께 검증한다.
- upload root는 absolute normalize한다.
- `uploadRoot.resolve(storageKey).normalize()` 결과가 upload root 밖으로 나가면 거부한다.
- upload DB 저장 실패 시 orphan file best-effort 보상 삭제를 수행한다.
- 보상 삭제 실패가 원래 DB 예외를 가리지 않도록 처리한다.

### ImageUrlParser

- lifecycle attach/sync 대상은 `/images/{uuid}.{ext}` 형태만 인정한다.
- `{uuid}`는 UUID 형식이어야 한다.
- 허용 확장자는 `png`, `jpg`, `jpeg`, `gif`, `webp`다.
- 외부 URL은 무시한다.
- protocol-relative URL은 무시한다.
- query string과 fragment가 있는 URL은 무시한다.
- encoded path, slash, backslash, `..`, `%`, `?`, `#`가 포함된 storageKey는 무시한다.
- 중복 storageKey는 제거한다.

### Attach / Sync

- `ImageServiceImpl.attachImages`는 전달된 `storageKeys`를 distinct 처리한다.
- distinct 처리 후 repository 조회 결과 수가 distinct key 수와 다르면 존재하지 않거나 권한이 없는 이미지가 포함된 것으로 보고 예외를 던진다.
- service layer는 `Image.isAttachable()` 필터에 의존하지 않는다.
- attach 가능 여부, 멱등성, 상태 위반 검증은 `Image.attach(ownerId, ownerType)` 도메인 메서드가 담당한다.
- `PENDING` 이미지는 owner에 attach되어 `USED`가 될 수 있다.
- `DETACHED` 이미지는 다시 attach되어 `USED`가 될 수 있다.
- 이미 같은 owner에 `USED` 상태로 attach된 이미지는 재처리되어도 성공해야 한다.
- 다른 owner에 이미 attach된 이미지 등 상태 위반은 `Image.attach()`에서 막는다.
- Outbox 재처리 시 동일 owner attach는 멱등적으로 처리되어야 한다.
- `syncImages(ownerId, ownerType, newKeys, uploaderId)`는 기존 owner 이미지 중 새 key에 없는 이미지를 detach한다.
- Post delete image detach는 `syncImages(postId, POST, emptyList, userId)`로 처리한다.

### Cleanup

- `DETACHED` cleanup 기준은 `detachedAt`이다.
- `PENDING` cleanup 기준은 `createdAt`이다.
- cleanup job은 파일 삭제 성공 image만 `DELETED` 처리한다.
- 파일이 이미 없는 경우는 cleanup 관점에서 idempotent success로 볼 수 있다.
- 파일 삭제 실패는 호출자에게 전파한다.

## 테스트 기준

- ImageUrlParser는 `/images/{uuid}.{ext}`만 추출하는지 검증한다.
- 외부 URL, query, fragment, encoded path, slash/backslash, `..` 포함 URL은 무시하는지 검증한다.
- upload는 확장자, contentType, magic byte, SVG 금지를 검증한다.
- path traversal 방어를 검증한다.
- DB 저장 실패 시 orphan file 보상 삭제를 검증한다.
- cleanup은 파일 삭제 성공 image만 `DELETED` 처리하는지 검증한다.
- attachImages는 distinct, 동일 owner 멱등성, DETACHED reattach, 다른 owner 거부, storageKey mismatch를 검증한다.

## TODO / 운영 전 개선

- 5MB 초과 multipart 직접 업로드가 service layer에 도달하기 전에 500으로 응답할 수 있는지 확인 필요.
- 운영 전 multipart size 초과 예외를 명시적 400 또는 413 정책 응답으로 고정한다.
