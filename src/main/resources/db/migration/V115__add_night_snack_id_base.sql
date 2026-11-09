-- append-only 선착순 전략의 순번 기준점.
--
-- 이 전략은 정원 카운터를 쓰지 않고 순번을 AUTO_INCREMENT id 의 칸 번호로 정한다.
--   순번 = night_snack_application.id - night_snack.id_base + 1
-- 따라서 id_base 는 "그 마차의 첫 일반 신청이 받게 될 id" 여야 한다. open() 시점에
-- night_snack_application 의 다음 id 값을 박아 두고, 신청은 OPEN 상태에서만 받으므로
-- id_base 가 모든 일반 신청 id 보다 항상 작거나 같다는 것이 보장된다.
--
-- id_base 가 실제 첫 id 보다 크면 순번이 실제보다 낮아져 정원을 초과하므로, 커밋된 행의
-- MIN(id) 로 사후에 구해서는 안 된다(첫 행이 미커밋이면 MIN 이 실제보다 커진다).
--
-- append-only 를 쓰지 않는 전략과 이 마이그레이션 이전에 생성된 마차에서는 NULL 로 남는다.
ALTER TABLE night_snack
    ADD COLUMN id_base BIGINT NULL AFTER remaining;

-- 잔여 수량 표시용 인덱스.
-- 잔여 = 일반 정원 - (MAX(id) - id_base + 1) 이라서 조회가 한 마차·한 source 의 MAX(id) 하나로
-- 끝난다. 세 컬럼이 모두 인덱스에 있어 역방향 탐색 한 번이면 되고 테이블을 타지 않는다.
-- 기존 idx_night_snack_application_night_snack_status 는 (night_snack_id, status) 라 이 조회를
-- 커버하지 못한다.
CREATE INDEX idx_night_snack_application_slot
    ON night_snack_application (night_snack_id, source, id);
