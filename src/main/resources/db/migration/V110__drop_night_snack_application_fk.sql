-- 운영 편의를 위해 night_snack_application의 FK 제약을 제거합니다.
-- member 또는 night_snack 레코드가 삭제되어도 application이 고아 레코드로 남을 수 있습니다.
--
-- night_snack_id: 기존 idx_night_snack_application_night_snack_status (night_snack_id, status) 로 커버됨.
-- member_id: FK 인덱스만 있었으므로 명시 인덱스를 추가합니다.
ALTER TABLE night_snack_application
    DROP FOREIGN KEY fk_night_snack_application_member,
    DROP FOREIGN KEY fk_night_snack_application_night_snack,
    ADD INDEX idx_night_snack_application_member_id (member_id);
