INSERT INTO users (email, password, nickname, role, user_status, account_status, email_verified, deleted, created_at, updated_at, introduce, profile_image_url)
VALUES ('super@super.com', '{bcrypt}$2a$10$2Jk..KA5IiOebrrUgexVAOnY42jqryCRbMKT6FIWTXYJean7w2hUS', 'super', 'ADMIN', 'ACTIVE', 'NORMAL', true, false, NOW(), NOW(), '자기소개를 입력해주세요.', 'uploads/default.png');

INSERT INTO users (email, password, nickname, role, user_status, account_status, email_verified, deleted, created_at, updated_at, introduce, profile_image_url)
VALUES ('rhkdgh821@naver.com', '{bcrypt}$2a$10$fVn4lhyxvKSwaHQja3L4D.fTshkPTdWSEN8q9yEZ6OrmzwfoKwwB6', 'rhkdgh821', 'USER', 'PENDING', 'NORMAL', true, false, NOW(), NOW(), '자기소개를 입력해주세요.', 'uploads/default.png');

INSERT INTO board (name, description, board_status, created_at, updated_at)
VALUES ('공지사항', '공지사항입니다.', 'ACTIVE', NOW(), NOW());

INSERT INTO post (board_id, user_id, title, content, deleted, like_count, view_count, created_at, updated_at)
VALUES (1, 1, '첫 게시물', '첫 게시물 입니다.', false, 0, 0, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, deleted, created_at, updated_at)
VALUES (1, 1, '첫번째 댓글', null, 0, false, NOW(), NOW());

INSERT INTO comment (post_id, user_id, content, parent_id, depth, deleted, created_at, updated_at)
VALUES (1, 1, '첫번째 대댓글', 1, 1, false, NOW(), NOW());

