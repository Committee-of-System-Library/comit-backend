-- Discussion #96 + 요구사항 4-3: 선착순 신청(모델 A + 방안 1 DB 원자적 UPDATE) + 사전신청 예약분.
--
-- night_snack: 하루 단위 선착순 마차. 전체 정원 capacity = 예약분(reserved_capacity) + 일반분.
--   remaining          : 일반 선착순 잔여(= 일반분 capacity - reserved_capacity 의 잔여, 원자적 UPDATE로 감소)
--   reserved_capacity  : 사전신청용으로 떼어 둔 정원(기본 10%, 생성 시 관리자가 지정)
--   reserved_remaining : 예약분 잔여 — 사전신청 1건마다 감소(일반분 remaining과 분리)

CREATE TABLE night_snack
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    night_snack_date   DATE        NOT NULL,
    capacity           INT         NOT NULL,
    remaining          INT         NOT NULL,
    reserved_capacity  INT         NOT NULL DEFAULT 0,
    reserved_remaining INT         NOT NULL DEFAULT 0,
    status             VARCHAR(20) NOT NULL,
    created_at         DATETIME(6) NOT NULL,

    UNIQUE KEY uk_night_snack_date (night_snack_date)
);

-- night_snack_application: 신청 1건.
--   member_id      : 일반 선착순은 필수, 사전신청(학번만)은 Comit 계정이 없을 수 있어 NULL 허용
--   student_number : 사전신청은 필수, 일반은 회원 학번을 복사(없으면 NULL)
--   source         : GENERAL(일반 선착순) / RESERVED(사전신청)
CREATE TABLE night_snack_application
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id      BIGINT,
    night_snack_id BIGINT      NOT NULL,
    student_number VARCHAR(20),
    source         VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    ticket_token   VARCHAR(36) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    confirmed_at   DATETIME(6),

    -- 1인 1마차 1회(일반 선착순). 동시 중복 신청을 막는 방어선. member_id NULL(사전신청)은 다중 허용된다.
    UNIQUE KEY uk_night_snack_application_member_night_snack (member_id, night_snack_id),
    UNIQUE KEY uk_night_snack_application_ticket_token (ticket_token),
    -- (night_snack_id, student_number) 유니크: 사전신청끼리의 중복뿐 아니라 "사전신청된 학번이 같은 마차의
    -- 일반 선착순으로 또 신청"하는 교차 중복까지 막는다. student_number NULL(학번 미보유 일반 회원)은
    -- MySQL 특성상 다중 NULL 이 허용되어 제약 대상이 아니다.
    UNIQUE KEY uk_night_snack_application_night_snack_student_number (night_snack_id, student_number),
    INDEX idx_night_snack_application_night_snack_status (night_snack_id, status),

    CONSTRAINT fk_night_snack_application_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_night_snack_application_night_snack FOREIGN KEY (night_snack_id) REFERENCES night_snack (id)
);
