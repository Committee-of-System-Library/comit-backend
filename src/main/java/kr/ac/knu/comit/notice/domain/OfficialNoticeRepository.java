package kr.ac.knu.comit.notice.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OfficialNoticeRepository extends JpaRepository<OfficialNotice, Long> {

    @Query("SELECT n FROM OfficialNotice n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<OfficialNotice> findActiveById(@Param("id") Long id);

    @Query("SELECT n.wrId FROM OfficialNotice n WHERE n.wrId IN :wrIds")
    Set<String> findExistingWrIds(@Param("wrIds") List<String> wrIds);

    @Query("""
            SELECT n FROM OfficialNotice n
            WHERE n.deletedAt IS NULL
            ORDER BY n.id DESC
            """)
    List<OfficialNotice> findFirstPage(Pageable pageable);

    @Query("""
            SELECT n FROM OfficialNotice n
            WHERE n.deletedAt IS NULL
              AND n.id < :cursorId
            ORDER BY n.id DESC
            """)
    List<OfficialNotice> findByCursor(@Param("cursorId") Long cursorId, Pageable pageable);
}
