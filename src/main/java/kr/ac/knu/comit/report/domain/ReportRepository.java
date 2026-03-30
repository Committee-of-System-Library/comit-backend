package kr.ac.knu.comit.report.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId
    );

    Optional<Report> findByIdAndDeletedAtIsNull(Long reportId);

    @Query("SELECT r FROM Report r WHERE r.deletedAt IS NULL "
            + "AND (:status IS NULL OR r.status = :status) "
            + "AND (:targetType IS NULL OR r.targetType = :targetType) "
            + "ORDER BY r.createdAt DESC")
    Page<Report> findAllActiveByFilters(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            Pageable pageable
    );
}
