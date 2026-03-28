package kr.ac.knu.comit.report.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId
    );
}
