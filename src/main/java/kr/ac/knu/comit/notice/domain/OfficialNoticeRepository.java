package kr.ac.knu.comit.notice.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfficialNoticeRepository extends JpaRepository<OfficialNotice, Long> {

    /**
     * 삭제되지 않은 공지사항 하나를 조회한다.
     */
    @Query("SELECT n FROM OfficialNotice n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<OfficialNotice> findActiveById(@Param("id") Long id);

    /**
     * cursor 없이 첫 페이지를 최신순으로 조회한다.
     */
    @Query("""
            SELECT n FROM OfficialNotice n
            WHERE n.deletedAt IS NULL
            ORDER BY n.id DESC
            """)
    List<OfficialNotice> findFirstPage(Pageable pageable);

    /**
     * cursor 기준으로 그 이전 ID를 최신순으로 조회한다.
     */
    @Query("""
            SELECT n FROM OfficialNotice n
            WHERE n.deletedAt IS NULL
              AND n.id < :cursorId
            ORDER BY n.id DESC
            """)
    List<OfficialNotice> findByCursor(@Param("cursorId") Long cursorId, Pageable pageable);
}
