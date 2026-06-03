-- 야식 마차 오픈/마감 시각 컬럼 추가 (Period 값 객체 매핑)
-- 스케줄러 또는 관리자 트리거가 이 시각을 기준으로 SCHEDULED → OPEN → CLOSED 전환을 수행한다.

ALTER TABLE night_snack
    ADD COLUMN open_at  DATETIME(6) NOT NULL AFTER reserved_remaining,
    ADD COLUMN close_at DATETIME(6) NOT NULL AFTER open_at;
