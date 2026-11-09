package kr.ac.knu.comit.nightsnack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.domain.Period;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;

/** 야식마차의 정원, 신청 기간과 접수 상태. 일반 잔여 수량은 신청 건수로 조회한다. */
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

    /** OPEN 전 저장한 신청 ID 하한. 최대 50%의 즉시 확정 구간에만 사용한다. */
    @Column(name = "id_base")
    private Long idBase;

    /** 사전신청용으로 떼어 둔 정원(요구사항 4-3, 기본 10% — 생성 시 관리자가 지정). */
    @Column(name = "reserved_capacity", nullable = false)
    private int reservedCapacity;

    /** 예약분 잔여 수량. 행사 배타 잠금을 보유한 사전신청 트랜잭션에서 감소한다. */
    @Column(name = "reserved_remaining", nullable = false)
    private int reservedRemaining;

    /** 오픈·마감 시각. 스케줄러 또는 관리자가 이 시각을 기준으로 상태 전이를 수행한다. */
    @Embedded
    private Period period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NightSnackStatus status;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String contents;

    @Column(length = 500)
    private String menu;

    @Column(name = "pickup_location", length = 200)
    private String pickupLocation;

    @Column(name = "pickup_start_time")
    private LocalDateTime pickupStartTime;

    @Column(name = "pickup_deadline")
    private LocalDateTime pickupDeadline;

    /** 신청자격: 학생회비 납부 여부. 등록하지 않으면 false. */
    @Column(name = "requires_student_council_fee", nullable = false)
    private boolean requiresStudentCouncilFee;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NightSnack() {
    }

    /**
     * 관리자 생성 전용 팩토리. 부가 정보(제목·내용·메뉴·수령장소·수령마감·신청자격)를 함께 등록한다.
     *
     * @param requiresStudentCouncilFee 신청자격 — 학생회비 납부 여부. 미등록 시 {@code false}.
     */
    public static NightSnack create(LocalDate nightSnackDate, int capacity, int reservedCapacity, Period period,
                                    String title, String contents, String menu,
                                    String pickupLocation, LocalDateTime pickupStartTime, LocalDateTime pickupDeadline,
                                    boolean requiresStudentCouncilFee) {
        if (nightSnackDate == null || capacity <= 0
                || reservedCapacity < 0 || reservedCapacity > capacity
                || period == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        NightSnack nightSnack = new NightSnack();
        nightSnack.nightSnackDate = nightSnackDate;
        nightSnack.capacity = capacity;
        nightSnack.reservedCapacity = reservedCapacity;
        nightSnack.reservedRemaining = reservedCapacity;
        nightSnack.period = period;
        nightSnack.status = NightSnackStatus.SCHEDULED;
        nightSnack.title = title;
        nightSnack.contents = contents;
        nightSnack.menu = menu;
        nightSnack.pickupLocation = pickupLocation;
        nightSnack.pickupStartTime = pickupStartTime;
        nightSnack.pickupDeadline = pickupDeadline;
        nightSnack.requiresStudentCouncilFee = requiresStudentCouncilFee;
        nightSnack.createdAt = LocalDateTime.now();
        return nightSnack;
    }

    /** OPEN 전이와 ID 하한 저장은 행사 배타 잠금을 보유한 같은 트랜잭션에서 수행한다. */
    public void open(long idBase) {
        if (this.status != NightSnackStatus.SCHEDULED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        this.status = NightSnackStatus.OPEN;
        this.idBase = idBase;
    }

    public void close() {
        this.status = NightSnackStatus.CLOSED;
    }

    public boolean isOpen() {
        return this.status == NightSnackStatus.OPEN;
    }

    public boolean isOpenAt(LocalDateTime now) {
        return period.contains(now);
    }

    public boolean isScheduled() {
        return this.status == NightSnackStatus.SCHEDULED;
    }

    /** 일반 선착순으로 받을 수 있는 정원(= 전체 정원에서 예약분을 뺀 값). */
    public int generalCapacity() {
        return capacity - reservedCapacity;
    }

    /** ID의 빈 번호는 즉시 확정 수만 줄인다. 최종 선정은 실제 신청 행을 정렬해 수행한다. */
    public boolean isInstantAccepted(Long applicationId) {
        return applicationId != null && idBase != null && applicationId >= idBase
                && applicationId - idBase < generalCapacity() / 2;
    }

    /** 관리자 예약 등록은 행사 배타 잠금을 보유한 상태에서 호출한다. */
    public boolean reserve(int count) {
        if (count <= 0 || count > reservedRemaining) {
            return false;
        }
        this.reservedRemaining -= count;
        return true;
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

    public int getReservedCapacity() {
        return reservedCapacity;
    }

    public int getReservedRemaining() {
        return reservedRemaining;
    }

    public Period getPeriod() {
        return period;
    }

    public NightSnackStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getContents() {
        return contents;
    }

    public String getMenu() {
        return menu;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public LocalDateTime getPickupDeadline() {
        return pickupDeadline;
    }

    public boolean isRequiresStudentCouncilFee() {
        return requiresStudentCouncilFee;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPickupStartTime() {
        return pickupStartTime;
    }
}
