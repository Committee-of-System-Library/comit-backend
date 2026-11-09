package kr.ac.knu.comit.nightsnack.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NightSnackApplicationRepository extends JpaRepository<NightSnackApplication, Long> {

    boolean existsByMemberIdAndNightSnackId(Long memberId, Long nightSnackId);

    Optional<NightSnackApplication> findByMemberIdAndNightSnackId(Long memberId, Long nightSnackId);

    long countByNightSnackId(Long nightSnackId);

    /**
     * 해당 마차에 이미 신청된(사전신청 + 일반 선착순) 학번 목록. 사전신청 배치 등록 시 교차 중복을
     * 사전 필터링하는 데 쓴다. 학번이 없는 일반 신청(null)은 제외한다.
     */
    @Query("""
            SELECT a.studentNumber FROM NightSnackApplication a
            WHERE a.nightSnack.id = :nightSnackId AND a.studentNumber IS NOT NULL
            """)
    List<String> findStudentNumbersByNightSnackId(@Param("nightSnackId") Long nightSnackId);

    Optional<NightSnackApplication> findByNightSnackIdAndStudentNumber(Long nightSnackId, String studentNumber);

    /** OPEN 전에만 조회한다. 실제 다음 ID보다 작아도 즉시 확정 구간이 줄어들 뿐이다. */
    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM night_snack_application", nativeQuery = true)
    long nextId();

    long countByNightSnackIdAndSource(Long nightSnackId, NightSnackApplicationSource source);

    @Query("""
            SELECT a.id FROM NightSnackApplication a
            WHERE a.nightSnack.id = :nightSnackId
              AND a.source = kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationSource.GENERAL
            ORDER BY a.id ASC
            """)
    List<Long> findGeneralCandidateIds(@Param("nightSnackId") Long nightSnackId, Pageable pageable);

    /** 이미 확정된 기존 데이터는 보존하고, 대기 신청을 한 번의 UPDATE로 최종 선정한다. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE night_snack_application
            SET selection_status = CASE WHEN id <= :lastAcceptedId THEN 'ACCEPTED' ELSE 'REJECTED' END
            WHERE night_snack_id = :nightSnackId AND source = 'GENERAL' AND selection_status = 'PENDING'
            """, nativeQuery = true)
    int finalizeSelections(@Param("nightSnackId") Long nightSnackId, @Param("lastAcceptedId") long lastAcceptedId);
}
