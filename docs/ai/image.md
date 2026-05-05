# Image Domain AI Rules

## 목적

Image 문서는 업로드, 저장, URL 파싱, attach/sync, cleanup, 파일 삭제 안정성 정책을 정리한다.

이 문서는 Image 관련 작업에서 우선 참조한다.

---

## Security / Endpoint Policy

- Image upload API는 authenticated가 필요하다.
- Image upload path를 permitAll로 열지 않는다.
- 정적 이미지 조회 경로와 이미지 업로드 API 권한을 혼동하지 않는다.

---

## Image 상태 정책

- Image 상태는 `PENDING`, `USED`, `DETACHED`, `DELETED`를 사용한다.
- `PENDING`은 업로드됐지만 아직 owner에 연결되지 않은 상태다.
- `USED`는 owner에 연결된 상태다.
- `DETACHED`는 더 이상 owner가 사용하지 않는 상태다.
- `DELETED`는 실제 파일 삭제 후 DB에 삭제 상태로 남긴 상태다.
- `DETACHED` cleanup 기준은 `createdAt`이 아니라 `detachedAt`이다.
- `PENDING` cleanup 기준은 `createdAt`을 사용한다.
- cleanup job은 파일 삭제 성공 id만 DB `DELETED` 처리한다.
- 파일 삭제 실패는 호출자에게 전파한다.

---

## Upload / LocalFileStorage 보안 정책

- 저장 파일명은 서버가 생성한 UUID + 허용 확장자만 사용한다.
- 원본 파일명은 확장자 추출 용도로만 사용하고 저장 경로에 직접 반영하지 않는다.
- 허용 확장자는 `jpg`, `jpeg`, `png`, `gif`, `webp`다.
- SVG는 허용하지 않는다.
- 허용 contentType은 `image/jpeg`, `image/png`, `image/gif`, `image/webp`다.
- `ImageUploadServiceImpl`은 contentType allowlist와 magic byte 검증을 함께 수행한다.
- `ImageUploadServiceImpl`은 PNG, JPEG, GIF, WebP의 최소 signature를 검증한다.
- 서비스 코드의 5MB 제한과 multipart `max-file-size`, `max-request-size` 제한을 함께 유지한다.
- `LocalFileStorage`는 실제 파일 저장/삭제를 담당한다.
- `LocalFileStorage`는 upload root를 absolute normalize한다.
- `LocalFileStorage`는 `uploadRoot.resolve(storageKey).normalize()` 결과가 upload root 밖으로 나가지 않도록 `startsWith`로 검증한다.
- 업로드 파일 저장 후 DB 저장 실패 시 best-effort 보상 삭제를 수행한다.
- 보상 삭제 실패는 원래 DB 예외를 가리지 않도록 처리한다.

---

## ImageUrlParser 정책

- `ImageUrlParser`는 `/images/{uuid}.{ext}` 형태의 내부 URL만 attach/sync 대상으로 인정한다.
- `{uuid}`는 UUID 형식이어야 한다.
- 허용 확장자는 `png`, `jpg`, `jpeg`, `gif`, `webp`다.
- 외부 URL은 attach/sync 대상에서 무시한다.
- 절대 URL, protocol-relative URL, query string, fragment는 무시한다.
- URL 중간에 `/images/`가 있는 경우는 무시한다.
- encoded path, slash, backslash, `..`, `%`, `?`, `#`가 포함된 storageKey는 무시한다.
- 중복 storageKey는 제거한다.
- 외부 이미지는 본문에 남을 수 있지만, 내부 Image lifecycle attach/sync 대상은 아니다.

---

## Cleanup 정책

- cleanup job은 삭제 성공한 image id만 `DELETED` 처리한다.
- 파일이 이미 없는 경우는 cleanup 관점에서 idempotent success로 볼 수 있다.
- 파일 삭제 실패 image는 `DELETED` 처리 대상에서 제외한다.
- `DETACHED` 이미지는 `detachedAt` 기준 threshold를 사용한다.
- 오래전에 생성됐지만 방금 detached된 이미지는 cleanup 대상이 아니어야 한다.

---

## Testing Policy

- ImageUrlParser는 내부 `/images/{uuid}.{ext}`만 추출하는지 검증한다.
- 외부 URL, query, fragment, encoded path, slash/backslash/`..` 포함 URL은 무시되는지 검증한다.
- 업로드는 허용 확장자와 contentType만 통과하는지 검증한다.
- SVG가 거부되는지 검증한다.
- magic byte가 맞지 않는 파일이 거부되는지 검증한다.
- 저장 경로가 upload root 밖으로 나가지 않는지 검증한다.
- 파일 삭제 실패가 호출자에게 전파되는지 검증한다.
- DB 저장 실패 시 보상 삭제가 호출되는지 검증한다.
- cleanup은 파일 삭제 성공 image만 `DELETED` 처리하는지 검증한다.
