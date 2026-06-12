package kr.ac.knu.comit.notice.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfficialNoticeRepository extends JpaRepository<OfficialNotice, Long> {

    @Query("SELECT n FROM OfficialNotice n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<OfficialNotice> findActiveById(@Param("id") Long id);

    @Query("SELECT n.wrId FROM OfficialNotice n WHERE n.wrId IN :wrIds")
    Set<String> findExistingWrIds(@Param("wrIds") List<String> wrIds);

    @Query("""
            SELECT n FROM OfficialNotice n
            WHERE n.deletedAt IS NULL
            ORDER BY n.postedAt DESC, n.id DESC
            """)
    List<OfficialNotice> findFirstPage(Pageable pageable);

    @Query("""
            SELECT n FROM OfficialNotice n
            WHERE n.deletedAt IS NULL
              AND (
                  (:cursorPostedAt IS NOT NULL AND (
                      n.postedAt < :cursorPostedAt
                      OR n.postedAt IS NULL
                      OR (n.postedAt = :cursorPostedAt AND n.id < :cursorId)
                  ))
                  OR (:cursorPostedAt IS NULL AND n.postedAt IS NULL AND n.id < :cursorId)
              )
            ORDER BY n.postedAt DESC, n.id DESC
            """)
    List<OfficialNotice> findByCursor(
            @Param("cursorPostedAt") LocalDateTime cursorPostedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM OfficialNotice n
            WHERE n.deletedAt IS NULL
            ORDER BY n.id ASC
            """)
    List<OfficialNotice> findAllActive();

    @Query("""
            SELECT n.id AS id, n.summary AS summary
            FROM OfficialNotice n
            WHERE n.deletedAt IS NULL
              AND n.id IN :ids
            """)
    List<NoticeSummaryView> findSummariesByIds(@Param("ids") List<Long> ids);

    interface NoticeSummaryView {
        Long getId();

        String getSummary();
    }
}
