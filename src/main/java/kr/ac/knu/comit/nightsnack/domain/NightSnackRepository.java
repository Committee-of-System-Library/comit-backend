package kr.ac.knu.comit.nightsnack.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NightSnackRepository extends JpaRepository<NightSnack, Long> {

    Optional<NightSnack> findByNightSnackDate(LocalDate nightSnackDate);

    List<NightSnack> findAllByOrderByNightSnackDateDesc();

    /**
     * Discussion #96 "방안 1 — DB 원자적 UPDATE 선점 전략"의 핵심 쿼리.
     *
     * <p>잔여 수량 조회와 감소를 두 단계로 분리하지 않고 단일 UPDATE로 합쳐 Race Condition을
     * 제거한다. {@code status = OPEN} 과 {@code remaining > 0} 을 WHERE에 함께 두기 때문에
     * 반환값이 0이면 "마감" 또는 "오픈 전/마감됨" 둘 다일 수 있다 → 서비스에서 재조회로 구분한다.
     *
     * @return 갱신된 행 수. 1이면 선점 성공, 0이면 선점 실패.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE NightSnack e
            SET e.remaining = e.remaining - 1
            WHERE e.id = :nightSnackId AND e.status = kr.ac.knu.comit.nightsnack.domain.NightSnackStatus.OPEN AND e.remaining > 0
            """)
    int decrementRemaining(@Param("nightSnackId") Long nightSnackId);

    /**
     * time-based 전략용 원자적 감소. status 대신 openAt/closeAt 시간 범위로 신청 가능 여부를 판단한다.
     * {@code open_at <= :now < close_at} 구간에만 선점을 허용한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE NightSnack e
            SET e.remaining = e.remaining - 1
            WHERE e.id = :nightSnackId AND e.period.openAt <= :now AND e.period.closeAt > :now AND e.remaining > 0
            """)
    int decrementRemainingByTime(@Param("nightSnackId") Long nightSnackId, @Param("now") LocalDateTime now);
}
