-- DEV/STAGING 전용 테스트 계정 시드 데이터
-- 이 파일은 application-local.yml / application-staging.yml 의 flyway.locations 에만 포함됩니다.
-- prod 환경에 절대 포함하지 마세요.

INSERT IGNORE INTO member (sso_sub, nickname, name, phone, student_number, major_track, student_number_visible, status, created_at, agreed_at)
VALUES
    ('dev-admin-001', '관리자', '관리자', '010-0000-0000', '2020000001', null, true, 'ACTIVE', NOW(), NOW()),
    ('dev-user-001',  '일반유저', '김철수', '010-0000-0001', '2021000001', null, true, 'ACTIVE', NOW(), NOW()),
    ('dev-user-002',  '테스트유저', '이영희', '010-0000-0002', '2022000001', null, true, 'ACTIVE', NOW(), NOW());
