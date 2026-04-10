-- ============================================================
-- Comit 내 활동 API 테스트용 목 데이터
-- ============================================================
-- 대상 엔드포인트:
--   GET /members/me/activity
--   GET /members/me/posts
--   GET /members/me/comments
--   GET /members/me/likes
--
-- Postman 헤더: X-Member-Sub: mock-dev-1
-- 로컬 포트: 53080
-- ============================================================

-- 기존 목 데이터 정리 (재실행 시 충돌 방지)
DELETE FROM post_like      WHERE member_id IN (SELECT id FROM member WHERE sso_sub IN ('mock-dev-1', 'mock-dev-2'));
DELETE FROM comment_like   WHERE member_id IN (SELECT id FROM member WHERE sso_sub IN ('mock-dev-1', 'mock-dev-2'));
DELETE FROM comment        WHERE member_id IN (SELECT id FROM member WHERE sso_sub IN ('mock-dev-1', 'mock-dev-2'));
DELETE FROM post_tag       WHERE post_id   IN (SELECT id FROM post   WHERE member_id IN (SELECT id FROM member WHERE sso_sub IN ('mock-dev-1', 'mock-dev-2')));
DELETE FROM post_image     WHERE post_id   IN (SELECT id FROM post   WHERE member_id IN (SELECT id FROM member WHERE sso_sub IN ('mock-dev-1', 'mock-dev-2')));
DELETE FROM post           WHERE member_id IN (SELECT id FROM member WHERE sso_sub IN ('mock-dev-1', 'mock-dev-2'));
DELETE FROM member         WHERE sso_sub   IN ('mock-dev-1', 'mock-dev-2');

-- ============================================================
-- 1. 회원
-- ============================================================

-- 테스트 주체: mock-dev-1 (Postman에서 X-Member-Sub: mock-dev-1 로 사용)
INSERT INTO member (sso_sub, nickname, student_number, name, phone, profile_image_url,
                    student_number_visible, status, agreed_at, created_at)
VALUES ('mock-dev-1', 'backend-dev', '2020111111', '홍길동', '010-1234-5678', NULL,
        1, 'ACTIVE', '2024-03-02 09:00:00', '2024-03-02 09:00:00');

-- 다른 글 작성자 (내가 좋아요할 게시글의 원작자)
INSERT INTO member (sso_sub, nickname, student_number, name, phone, profile_image_url,
                    student_number_visible, status, agreed_at, created_at)
VALUES ('mock-dev-2', 'orm-master', '2019222222', '이순신', '010-9876-5432', NULL,
        1, 'ACTIVE', '2024-03-01 09:00:00', '2024-03-01 09:00:00');

-- ============================================================
-- 2. 게시글 (내가 쓴 것 5개 + 다른 사람 게시글 5개)
-- ============================================================

-- [내가 쓴 게시글] mock-dev-1 작성
INSERT INTO post (member_id, board_type, title, content, like_count, view_count,
                  hidden_by_admin, created_at, updated_at)
VALUES
    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     'QNA', 'JPA fetch join 질문',
     'join fetch와 entity graph 차이가 궁금합니다. 성능 차이도 있나요?',
     3, 128, 0, '2026-04-01 10:00:00', NULL),

    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     'FREE', '스프링 시큐리티 이슈',
     'JWT 필터 체인 순서 때문에 계속 403이 나네요.',
     1, 45, 0, '2026-03-28 09:30:00', NULL),

    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     'QNA', 'Testcontainers 설정법',
     'MySQL Testcontainers 로컬에서 설정하는 방법 공유합니다.',
     5, 210, 0, '2026-03-25 14:00:00', NULL),

    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     'FREE', 'QueryDSL 프로젝트 적용 후기',
     'JPQL 대비 QueryDSL이 확실히 타입 세이프티가 좋네요.',
     7, 320, 0, '2026-03-20 11:00:00', NULL),

    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     'QNA', 'N+1 문제 해결 경험담',
     'Batch size 설정 vs fetch join 뭐가 더 나을까요?',
     2, 88, 0, '2026-03-15 16:00:00', NULL);

-- [다른 사람 게시글] mock-dev-2 작성 (내가 좋아요할 대상)
INSERT INTO post (member_id, board_type, title, content, like_count, view_count,
                  hidden_by_admin, created_at, updated_at)
VALUES
    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-2'),
     'QNA', 'Redis 캐싱 전략 정리',
     'Cache-aside, Write-through 패턴 비교 정리입니다.',
     12, 540, 0, '2026-04-02 08:00:00', NULL),

    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-2'),
     'FREE', 'Kotlin 코루틴 입문',
     'suspend, launch, async 개념 정리했습니다.',
     8, 300, 0, '2026-04-01 14:00:00', NULL),

    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-2'),
     'QNA', 'JPA 영속성 컨텍스트 완전 정리',
     '1차 캐시, 쓰기 지연, 변경 감지 흐름입니다.',
     15, 680, 0, '2026-03-30 10:00:00', NULL),

    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-2'),
     'QNA', 'JVM 튜닝 실전 가이드',
     'GC 로그 분석부터 힙 사이즈 조정까지.',
     6, 190, 0, '2026-03-22 13:00:00', NULL),

    -- 삭제된 게시글 (좋아요 목록에서 제외되는지 확인용)
    ((SELECT id FROM member WHERE sso_sub = 'mock-dev-2'),
     'FREE', '(삭제된 게시글)',
     '이 글은 삭제 처리되어 좋아요 목록에서 보이지 않아야 합니다.',
     0, 0, 0, '2026-03-10 10:00:00', '2026-03-11 10:00:00');

-- 마지막 게시글 삭제 처리
UPDATE post
SET deleted_at = '2026-03-11 10:00:00'
WHERE title = '(삭제된 게시글)'
  AND member_id = (SELECT id FROM member WHERE sso_sub = 'mock-dev-2');

-- ============================================================
-- 3. 태그
-- ============================================================
INSERT INTO post_tag (post_id, name)
VALUES
    -- 내가 쓴 게시글 태그
    ((SELECT id FROM post WHERE title = 'JPA fetch join 질문'),          'spring'),
    ((SELECT id FROM post WHERE title = 'JPA fetch join 질문'),          'jpa'),
    ((SELECT id FROM post WHERE title = '스프링 시큐리티 이슈'),           'spring'),
    ((SELECT id FROM post WHERE title = '스프링 시큐리티 이슈'),           'security'),
    ((SELECT id FROM post WHERE title = 'Testcontainers 설정법'),        'test'),
    ((SELECT id FROM post WHERE title = 'Testcontainers 설정법'),        'docker'),
    ((SELECT id FROM post WHERE title = 'QueryDSL 프로젝트 적용 후기'),   'querydsl'),
    ((SELECT id FROM post WHERE title = 'QueryDSL 프로젝트 적용 후기'),   'jpa'),
    ((SELECT id FROM post WHERE title = 'N+1 문제 해결 경험담'),          'jpa'),
    ((SELECT id FROM post WHERE title = 'N+1 문제 해결 경험담'),          'performance'),
    -- 다른 사람 게시글 태그
    ((SELECT id FROM post WHERE title = 'Redis 캐싱 전략 정리'),          'redis'),
    ((SELECT id FROM post WHERE title = 'Redis 캐싱 전략 정리'),          'cache'),
    ((SELECT id FROM post WHERE title = 'Kotlin 코루틴 입문'),            'kotlin'),
    ((SELECT id FROM post WHERE title = 'JPA 영속성 컨텍스트 완전 정리'), 'jpa'),
    ((SELECT id FROM post WHERE title = 'JVM 튜닝 실전 가이드'),          'jvm'),
    ((SELECT id FROM post WHERE title = 'JVM 튜닝 실전 가이드'),          'performance');

-- ============================================================
-- 4. 댓글 (내가 쓴 댓글 6개 — 다른 사람 게시글에 달린 것들)
-- ============================================================
INSERT INTO comment (post_id, member_id, parent_comment_id, content, like_count,
                     hidden_by_admin, created_at, updated_at)
VALUES
    -- 다른 사람 게시글에 단 댓글
    ((SELECT id FROM post WHERE title = 'Redis 캐싱 전략 정리'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     NULL, '실제 프로젝트에 적용해봤는데 Cache-aside 패턴이 제일 무난하더라고요.',
     4, 0, '2026-04-03 09:00:00', NULL),

    ((SELECT id FROM post WHERE title = 'JPA 영속성 컨텍스트 완전 정리'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     NULL, '변경 감지 부분이 항상 헷갈렸는데 정리 감사합니다!',
     2, 0, '2026-04-02 11:00:00', NULL),

    ((SELECT id FROM post WHERE title = 'JVM 튜닝 실전 가이드'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     NULL, '좋은 자료네요. GC 알고리즘 비교도 추가해주시면 좋겠어요.',
     1, 0, '2026-03-30 21:00:00', NULL),

    ((SELECT id FROM post WHERE title = 'Kotlin 코루틴 입문'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     NULL, '저도 같은 문제 겪었어요. structured concurrency도 한번 다뤄주세요.',
     0, 0, '2026-04-01 08:00:00', NULL),

    ((SELECT id FROM post WHERE title = 'Redis 캐싱 전략 정리'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     NULL, 'TTL 설정 전략도 궁금한데 추가 글 써주실 수 있나요?',
     3, 0, '2026-03-29 15:00:00', NULL),

    -- 내 게시글에 달린 대댓글 (댓글 목록에 포함되어야 함)
    ((SELECT id FROM post WHERE title = 'JPA fetch join 질문'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     NULL, '추가로 Batch size도 같이 비교해봤는데 케이스마다 다르더라고요.',
     0, 0, '2026-03-26 10:00:00', NULL);

-- ============================================================
-- 5. 좋아요 (내가 다른 사람 게시글에 누른 것 4개 — 삭제 게시글 포함)
-- ============================================================
INSERT INTO post_like (post_id, member_id, created_at)
VALUES
    ((SELECT id FROM post WHERE title = 'Redis 캐싱 전략 정리'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     '2026-04-03 10:00:00'),

    ((SELECT id FROM post WHERE title = 'Kotlin 코루틴 입문'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     '2026-04-02 18:00:00'),

    ((SELECT id FROM post WHERE title = 'JPA 영속성 컨텍스트 완전 정리'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     '2026-04-01 15:00:00'),

    -- 삭제된 게시글에 누른 좋아요 → /members/me/likes 목록에서 제외되어야 함
    ((SELECT id FROM post WHERE title = '(삭제된 게시글)'),
     (SELECT id FROM member WHERE sso_sub = 'mock-dev-1'),
     '2026-03-11 09:00:00');

-- ============================================================
-- 결과 확인 쿼리
-- ============================================================
-- SELECT * FROM member         WHERE sso_sub IN ('mock-dev-1', 'mock-dev-2');
-- SELECT id, title, deleted_at FROM post WHERE member_id IN (SELECT id FROM member WHERE sso_sub IN ('mock-dev-1', 'mock-dev-2'));
-- SELECT c.id, c.content, p.title AS post_title FROM comment c JOIN post p ON c.post_id = p.id WHERE c.member_id = (SELECT id FROM member WHERE sso_sub = 'mock-dev-1');
-- SELECT pl.id, pl.created_at, p.title, p.deleted_at FROM post_like pl JOIN post p ON pl.post_id = p.id WHERE pl.member_id = (SELECT id FROM member WHERE sso_sub = 'mock-dev-1');
