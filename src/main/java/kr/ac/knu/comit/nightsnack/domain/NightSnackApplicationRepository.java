package kr.ac.knu.comit.nightsnack.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * {@code skip-locked} 전략의 좌석 선점 1단계. {@code FOR UPDATE SKIP LOCKED} 는 JPQL이 표현하지 못해
     * 네이티브 쿼리로 작성한다 — 이미 락이 걸린 좌석(다른 트랜잭션이 선점 중)은 건너뛰고 바로 다음
     * AVAILABLE 좌석을 반환하므로, 여러 트랜잭션이 대기(blocking) 없이 서로 다른 행을 병렬 선점한다.
     *
     * <p>{@code night_snack} 테이블은 조인하지 않는다 — 조인하면 MySQL은 조인된 모든 테이블의 행을
     * 잠그는데(테이블별 {@code FOR UPDATE OF} 미지원), {@code night_snack} 은 이벤트당 단일 행이라
     * 조인 즉시 이 전략이 없애려는 "단일 핫 row" 경합이 되살아난다. 오픈 시간 판정은 이미 로딩된
     * {@link NightSnack} 엔티티로 호출부에서 락 없이 수행한다.
     *
     * @return 락을 획득한 좌석의 id. 반환값이 없으면(Optional#empty) 마감.
     */
    @Query(value = """
            SELECT id FROM night_snack_application
            WHERE night_snack_id = :nightSnackId AND status = 'AVAILABLE'
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Long> findAvailableSeatIdForUpdateSkipLocked(@Param("nightSnackId") Long nightSnackId);

    /**
     * 좌석 선점 2단계. 1단계에서 {@code FOR UPDATE} 로 이미 X Lock을 쥔 행이므로 같은 트랜잭션 내에서는
     * 경합 없이 갱신된다. {@code WHERE status = 'AVAILABLE'} 가드는 방어적 재확인용이다.
     *
     * @return 갱신된 행 수. 1이면 선점 성공.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE night_snack_application
            SET member_id = :memberId, student_number = :studentNumber, status = 'PENDING'
            WHERE id = :seatId AND status = 'AVAILABLE'
            """, nativeQuery = true)
    int claimSeat(@Param("seatId") Long seatId, @Param("memberId") Long memberId,
                  @Param("studentNumber") String studentNumber);
}
