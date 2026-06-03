package kr.ac.knu.comit.nightsnack.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
