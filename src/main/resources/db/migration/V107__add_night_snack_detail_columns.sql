-- 야식 마차 부가 정보 컬럼 추가: 제목, 내용, 메뉴, 수령장소, 수령마감, 신청자격(학생회비 납부 여부)
-- 기존 행은 NULL / DEFAULT FALSE 로 자동 처리된다.

ALTER TABLE night_snack
    ADD COLUMN title                        VARCHAR(200) NULL          AFTER close_at,
    ADD COLUMN contents                     TEXT         NULL          AFTER title,
    ADD COLUMN menu                         VARCHAR(500) NULL          AFTER contents,
    ADD COLUMN pickup_location              VARCHAR(200) NULL          AFTER menu,
    ADD COLUMN pickup_deadline              DATETIME(6)  NULL          AFTER pickup_location,
    ADD COLUMN requires_student_council_fee BOOLEAN      NOT NULL DEFAULT FALSE AFTER pickup_deadline;
