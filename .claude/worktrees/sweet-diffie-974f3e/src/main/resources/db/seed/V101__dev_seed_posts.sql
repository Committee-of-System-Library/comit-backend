-- DEV/STAGING 전용 게시글 시드 데이터 (NOTICE, EVENT, QNA, FREE)
-- 이 파일은 application-local.yml / application-staging.yml 의 flyway.locations 에만 포함됩니다.

-- NOTICE 게시글
INSERT IGNORE INTO post (member_id, board_type, title, content, like_count, view_count, hidden_by_admin, created_at)
VALUES
    ((SELECT id FROM member WHERE sso_sub = 'dev-admin-001'),
     'NOTICE', '2026년 1학기 정기 모집 공고',
     '안녕하세요, 경북대 컴퓨터학부 시스템 소프트웨어 학술 동아리 COMIT입니다.\n\n2026년 1학기 신입 부원을 모집합니다.\n\n- 모집 대상: 경북대학교 재학생 누구나\n- 모집 기간: 2026.03.01 ~ 2026.03.15\n- 지원 방법: 구글 폼 작성 후 제출\n\n많은 지원 바랍니다!',
     0, 0, 0, NOW()),

    ((SELECT id FROM member WHERE sso_sub = 'dev-admin-001'),
     'NOTICE', '정기 세미나 일정 안내 (3월)',
     '3월 정기 세미나 일정을 안내드립니다.\n\n- 일시: 2026년 3월 20일 (금) 오후 6시\n- 장소: IT대학 108호\n- 주제: Spring Boot 실전 프로젝트\n\n참석 희망자는 사전 신청 바랍니다.',
     2, 15, 0, NOW()),

    ((SELECT id FROM member WHERE sso_sub = 'dev-admin-001'),
     'NOTICE', '동아리방 이용 규칙 안내',
     '동아리방 이용 시 다음 규칙을 준수해주세요.\n\n1. 이용 후 청결 유지\n2. 장비 무단 반출 금지\n3. 야간 이용은 사전 허가 필요\n\n위반 시 이용이 제한될 수 있습니다.',
     1, 30, 0, NOW());

-- EVENT 게시글
INSERT IGNORE INTO post (member_id, board_type, title, content, like_count, view_count, hidden_by_admin, created_at)
VALUES
    ((SELECT id FROM member WHERE sso_sub = 'dev-admin-001'),
     'EVENT', '2026 해커톤 참가자 모집',
     'COMIT 주최 해커톤에 여러분을 초대합니다!\n\n- 일시: 2026년 4월 5일 ~ 6일 (24시간)\n- 장소: IT대학 세미나실\n- 팀 구성: 3~4인\n- 시상: 대상(50만원), 최우수상(30만원), 우수상(10만원)\n\n참가 신청: 구글 폼을 통해 신청해주세요.',
     5, 80, 0, NOW()),

    ((SELECT id FROM member WHERE sso_sub = 'dev-admin-001'),
     'EVENT', 'CS 스터디 그룹 모집',
     '알고리즘 & 자료구조 스터디 그룹을 모집합니다.\n\n- 대상: COMIT 부원\n- 인원: 5명 내외\n- 일정: 매주 수요일 오후 7시\n- 내용: 코딩테스트 준비, 백준 문제 풀이\n\n참여를 원하시면 카카오톡 채널로 연락주세요.',
     3, 45, 0, NOW()),

    ((SELECT id FROM member WHERE sso_sub = 'dev-admin-001'),
     'EVENT', '졸업 선배 네트워킹 데이',
     'COMIT 졸업 선배들과의 네트워킹 행사에 초대합니다.\n\n- 일시: 2026년 5월 10일 오후 2시\n- 장소: IT대학 로비\n- 내용: 취업/진로 상담, 프로젝트 경험 공유\n\n현재 취업 준비 중이거나 진로 고민이 있는 분들의 많은 참여 바랍니다.',
     7, 120, 0, NOW());

-- NOTICE 태그
INSERT IGNORE INTO post_tag (post_id, name)
SELECT p.id, t.name
FROM post p
JOIN (
    SELECT '2026년 1학기 정기 모집 공고' AS title, '모집' AS name
    UNION ALL SELECT '2026년 1학기 정기 모집 공고', '공지'
    UNION ALL SELECT '정기 세미나 일정 안내 (3월)', '세미나'
    UNION ALL SELECT '정기 세미나 일정 안내 (3월)', '공지'
    UNION ALL SELECT '동아리방 이용 규칙 안내', '공지'
) t ON p.title = t.title
WHERE p.board_type IN ('NOTICE', 'EVENT')
  AND p.deleted_at IS NULL;

-- EVENT 태그
INSERT IGNORE INTO post_tag (post_id, name)
SELECT p.id, t.name
FROM post p
JOIN (
    SELECT '2026 해커톤 참가자 모집' AS title, '해커톤' AS name
    UNION ALL SELECT '2026 해커톤 참가자 모집', '이벤트'
    UNION ALL SELECT 'CS 스터디 그룹 모집', '스터디'
    UNION ALL SELECT 'CS 스터디 그룹 모집', '알고리즘'
    UNION ALL SELECT '졸업 선배 네트워킹 데이', '네트워킹'
    UNION ALL SELECT '졸업 선배 네트워킹 데이', '취업'
) t ON p.title = t.title
WHERE p.board_type IN ('NOTICE', 'EVENT')
  AND p.deleted_at IS NULL;
