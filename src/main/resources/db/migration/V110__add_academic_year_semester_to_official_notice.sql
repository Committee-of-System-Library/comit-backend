ALTER TABLE official_notice
    ADD COLUMN academic_year SMALLINT NULL AFTER summary,
    ADD COLUMN semester      TINYINT  NULL AFTER academic_year;
