-- 야식 마차 수령 시작 시각 컬럼 추가
-- 기존 행은 NULL로 자동 처리된다.

ALTER TABLE night_snack
    ADD COLUMN pickup_start_time DATETIME(6) NULL AFTER pickup_location;
