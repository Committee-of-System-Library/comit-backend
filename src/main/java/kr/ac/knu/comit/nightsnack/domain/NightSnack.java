package kr.ac.knu.comit.nightsnack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;

/**
 * 선착순 신청 야식 마차(하루 단위). Discussion #96의 "모델 A"에 해당하며,
 * 정원과 잔여 수량을 DB 레벨에서 직접 관리한다.
 *
 * <p>동시성 처리는 "방안 1 — DB 원자적 UPDATE 선점"을 따른다. 즉 잔여 수량 감소는
 * 엔티티 setter가 아니라 {@link NightSnackRepository#decrementRemaining(Long)} 의 단일
 * 원자적 UPDATE로 처리한다. 이 엔티티의 {@code remaining} 필드는 조회/표시용 스냅샷이다.
 */
@Entity
@Table(name = "night_snack")
public class NightSnack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "night_snack_date", nullable = false, unique = true)
    private LocalDate nightSnackDate;

    @Column(nullable = false)
    private int capacity;

    /** 일반 선착순 잔여 수량(= 일반분 {@code capacity - reservedCapacity} 의 잔여). 원자적 UPDATE로 감소한다. */
    @Column(nullable = false)
    private int remaining;

    /** 사전신청용으로 떼어 둔 정원(요구사항 4-3, 기본 10% — 생성 시 관리자가 지정). */
    @Column(name = "reserved_capacity", nullable = false)
    private int reservedCapacity;

    /** 예약분 잔여 수량. 사전신청은 오픈 전 단일 관리자 흐름이라 일반분과 달리 원자적 UPDATE 없이 엔티티로 감소한다. */
    @Column(name = "reserved_remaining", nullable = false)
    private int reservedRemaining;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NightSnackStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NightSnack() {
    }

    /** 예약분 없는 야식 마차(전량 일반 선착순). */
    public static NightSnack create(LocalDate nightSnackDate, int capacity) {
        return create(nightSnackDate, capacity, 0);
    }

    /**
     * 정원을 예약분(사전신청)과 일반분(선착순)으로 나눠 생성한다(요구사항 4-3).
     *
     * @param reservedCapacity 사전신청용 예약 정원. {@code 0 <= reservedCapacity <= capacity} 여야 한다.
     */
    public static NightSnack create(LocalDate nightSnackDate, int capacity, int reservedCapacity) {
        if (nightSnackDate == null || capacity <= 0 || reservedCapacity < 0 || reservedCapacity > capacity) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        NightSnack nightSnack = new NightSnack();
        nightSnack.nightSnackDate = nightSnackDate;
        nightSnack.capacity = capacity;
        nightSnack.reservedCapacity = reservedCapacity;
        nightSnack.reservedRemaining = reservedCapacity;
        nightSnack.remaining = capacity - reservedCapacity; // 일반분 잔여
        nightSnack.status = NightSnackStatus.SCHEDULED;
        nightSnack.createdAt = LocalDateTime.now();
        return nightSnack;
    }

    /**
     * 신청 오픈. 17:30 정각에 누가 호출할지(관리자 토글 vs 스케줄러)는 미확정 — 초안에서는
     * 상태 전이 규칙만 도메인에 둔다.
     */
    public void open() {
        if (this.status != NightSnackStatus.SCHEDULED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        this.status = NightSnackStatus.OPEN;
    }

    public void close() {
        this.status = NightSnackStatus.CLOSED;
    }

    public boolean isOpen() {
        return this.status == NightSnackStatus.OPEN;
    }

    public boolean isScheduled() {
        return this.status == NightSnackStatus.SCHEDULED;
    }

    /** 일반 선착순으로 받을 수 있는 정원(= 전체 정원에서 예약분을 뺀 값). */
    public int generalCapacity() {
        return capacity - reservedCapacity;
    }

    /**
     * 사전신청으로 예약분을 {@code count} 만큼 선점한다(요구사항 4-3).
     *
     * <p>오픈 전 단일 관리자가 호출하는 흐름이라 경합이 없어 엔티티 필드 감소(JPA 변경 감지)로 처리한다.
     * 일반분({@link NightSnackRepository#decrementRemaining})과 달리 원자적 UPDATE를 쓰지 않는다.
     *
     * @return 예약 잔여분을 넘기지 않고 선점에 성공하면 {@code true}.
     */
    public boolean reserve(int count) {
        if (count <= 0 || count > reservedRemaining) {
            return false;
        }
        this.reservedRemaining -= count;
        return true;
    }

    /** 신청 순번(몇 번째) 계산용. 원자적 UPDATE 직후 갱신된 remaining 기준으로 호출한다. 일반분 기준 순번이다. */
    public int sequenceFromRemaining() {
        return generalCapacity() - remaining;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getNightSnackDate() {
        return nightSnackDate;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRemaining() {
        return remaining;
    }

    public int getReservedCapacity() {
        return reservedCapacity;
    }

    public int getReservedRemaining() {
        return reservedRemaining;
    }

    public NightSnackStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
