INSERT INTO users (email, password, nickname, role, user_status, account_status, is_email_verified, created_at, updated_at, introduce, profile_image_url, last_login_at)
VALUES ('super@super.com', '{bcrypt}$2a$10$2Jk..KA5IiOebrrUgexVAOnY42jqryCRbMKT6FIWTXYJean7w2hUS', 'super', 'ADMIN', 'ACTIVE', 'NORMAL', true, NOW(), NOW(), '자기소개를 입력해주세요.', 'uploads/default.png', NOW());

INSERT INTO users (email, password, nickname, role, user_status, account_status, is_email_verified, created_at, updated_at, introduce, profile_image_url, last_login_at)
VALUES ('rhkdgh821@naver.com', '{bcrypt}$2a$10$fVn4lhyxvKSwaHQja3L4D.fTshkPTdWSEN8q9yEZ6OrmzwfoKwwB6', 'rhkdgh821', 'USER', 'ACTIVE', 'NORMAL', true, NOW(), NOW(), '자기소개를 입력해주세요.', 'uploads/default.png', NOW());

INSERT INTO board (name, description, created_at, updated_at)
VALUES ('공지사항', '공지사항입니다.', NOW(), NOW());

INSERT INTO board (name, description, created_at, updated_at)
VALUES ('자유게시판', '자유게시판입니다.', NOW(), NOW());


-- 게시글 20개
INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (1, 1, '첫 게시물', '첫 게시물 입니다.', 3, 25, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (1, 1, '두번째 게시물', '두번째 게시물 입니다.', 7, 61, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (1, 1, '서비스 점검 안내', '오늘 밤 12시부터 서비스 점검이 진행됩니다.', 12, 180, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (1, 1, '업데이트 안내', '게시판 기능이 일부 개선되었습니다.', 9, 134, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (1, 1, '이용 규칙 안내', '게시판 이용 시 욕설과 광고성 글은 제한됩니다.', 15, 230, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, '오늘 날씨 좋네요', '산책하기 좋은 날씨입니다.', 2, 18, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, '스프링 공부 중입니다', 'Spring Security와 JWT를 공부하고 있습니다.', 11, 97, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, 'JPA 질문 있습니다', '연관관계 매핑을 하다가 궁금한 점이 생겼습니다.', 5, 73, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, 'QueryDSL 써보신 분?', '검색 조건이 많아져서 QueryDSL 도입을 고민 중입니다.', 8, 112, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, 'Redis는 언제 쓰나요?', '캐시와 세션 저장소로 Redis를 쓰는 이유가 궁금합니다.', 6, 88, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, 'AWS S3 이미지 업로드', '이미지를 S3에 저장하는 방식을 테스트하고 있습니다.', 13, 201, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, 'Nginx 배포 공부', 'Spring Boot 앞단에 Nginx를 두는 구조를 공부 중입니다.', 10, 156, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, '댓글 기능 구현 완료', '댓글과 대댓글 기능을 구현했습니다.', 4, 49, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, '좋아요 기능 테스트', '게시글 좋아요 기능을 테스트하고 있습니다.', 1, 14, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, '조회수 증가 로직', '조회수 중복 증가를 어떻게 막을지 고민 중입니다.', 7, 143, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, '테스트 데이터 추가', '개발용 테스트 데이터를 추가하고 있습니다.', 0, 9, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, '포트폴리오 게시판 프로젝트', '게시판 프로젝트를 포트폴리오 수준으로 다듬고 있습니다.', 16, 312, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, 'Outbox 패턴 적용 후기', '이벤트 처리 안정성을 위해 Outbox 패턴을 적용했습니다.', 14, 220, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, 'JWT 로그아웃 처리', 'Access Token과 Refresh Token 로그아웃 처리를 정리하고 있습니다.', 9, 118, NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, like_count, view_count, created_at, updated_at)
VALUES (2, 1, '배포 환경 구성 고민', 'EC2, Nginx, RDS, S3를 어떻게 구성할지 고민 중입니다.', 18, 405, NOW(), NOW());


-- 댓글
INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (1, 1, '첫번째 댓글', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (1, 1, '두번째 댓글', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (1, 1, '첫번째 대댓글', 1, 1, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (2, 1, '두번째 게시물 잘 봤습니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (2, 1, '내용이 도움이 됐어요.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (2, 1, '저도 같은 생각입니다.', 4, 1, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (3, 1, '점검 시간 확인했습니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (3, 1, '공지 감사합니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (5, 1, '이용 규칙 확인했습니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (7, 1, 'Spring Security 어렵지만 재밌죠.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (7, 1, 'JWT 필터 구조를 잡는 게 중요한 것 같아요.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (7, 1, '맞아요. 인증과 인가를 분리해서 보면 이해가 쉬워요.', 11, 1, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (8, 1, 'JPA 연관관계는 처음에 헷갈립니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (8, 1, '단방향부터 잡는 걸 추천합니다.', 13, 1, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (9, 1, '검색 조건이 많으면 QueryDSL이 확실히 편합니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (9, 1, '동적 쿼리에는 QueryDSL이 괜찮은 선택 같아요.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (10, 1, 'Redis는 Refresh Token 저장에도 많이 쓰입니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (11, 1, 'S3 key와 URL을 구분해서 저장하면 좋습니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (11, 1, 'CloudFront까지 붙이면 더 실무적인 구조가 됩니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (12, 1, 'Nginx reverse proxy 설정이 핵심입니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (12, 1, 'HTTPS는 Certbot으로 적용하면 편합니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (13, 1, '대댓글 depth 제한도 있으면 좋을 것 같아요.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (14, 1, '좋아요 중복 방지는 unique 제약조건이 중요합니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (15, 1, '조회수는 Redis로 중복 방지해도 좋을 것 같아요.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (17, 1, '포트폴리오로 충분히 좋아 보입니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (17, 1, '배포까지 하면 완성도가 확 올라갈 것 같아요.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (18, 1, 'Outbox는 실패 재처리가 핵심인 것 같습니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (18, 1, 'DEAD 상태 관리도 잘 해두면 운영 관점에서 좋습니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (19, 1, 'JWT 로그아웃은 Access Token 블랙리스트 여부가 고민되네요.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (20, 1, 'EC2에 먼저 단순 배포해보는 걸 추천합니다.', NULL, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, created_at, updated_at)
VALUES (20, 1, '그 다음 RDS, S3, Nginx 순서로 붙이면 좋을 것 같아요.', NULL, 0, NOW(), NOW());

