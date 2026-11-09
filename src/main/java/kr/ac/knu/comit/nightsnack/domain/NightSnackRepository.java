package kr.ac.knu.comit.nightsnack.domain;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NightSnackRepository extends JpaRepository<NightSnack, Long> {
    Optional<NightSnack> findFirstByNightSnackDateGreaterThanEqualOrderByNightSnackDateAsc(LocalDate nightSnackDate);
    List<NightSnack> findAllByOrderByNightSnackDateDesc();

    /** 접수끼리는 공유하고, 마감과는 배타적인 게이트. INSERT 커밋까지 유지한다. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT n FROM NightSnack n WHERE n.id = :id")
    Optional<NightSnack> findForApplication(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NightSnack n WHERE n.id = :id")
    Optional<NightSnack> findForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NightSnack n WHERE n.nightSnackDate = :date")
    Optional<NightSnack> findByDateForUpdate(@Param("date") LocalDate date);

    @Query("""
            SELECT n.id FROM NightSnack n
            WHERE n.status <> kr.ac.knu.comit.nightsnack.domain.NightSnackStatus.CLOSED
              AND n.period.openAt <= :now
            """)
    List<Long> findActiveIds(@Param("now") LocalDateTime now);
}
