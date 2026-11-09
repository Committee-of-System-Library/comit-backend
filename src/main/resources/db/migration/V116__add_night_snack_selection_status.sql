-- 기존 신청은 이미 수령 자격을 보장한 데이터이므로 ACCEPTED로 이관한다.
ALTER TABLE night_snack_application
    ADD COLUMN selection_status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED' AFTER status;
ALTER TABLE night_snack_application ALTER COLUMN selection_status SET DEFAULT 'PENDING';

-- 일반 잔여는 실제 접수 건수로 조회한다. 예약분 카운터는 유지한다.
ALTER TABLE night_snack DROP COLUMN remaining;
