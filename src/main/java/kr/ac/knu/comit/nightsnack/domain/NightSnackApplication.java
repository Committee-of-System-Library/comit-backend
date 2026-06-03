package kr.ac.knu.comit.nightsnack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.domain.Member;

/**
 * 선착순 신청 1건. {@code (member, nightSnack)} 조합은 유니크하며, 이 유니크 제약이 동시 중복
 * 신청을 막는 최종 방어선이다(애플리케이션 선검사는 빠른 차단용일 뿐 보장 수단이 아니다).
 *
 * <p>{@code ticketToken} 은 QR 수령 티켓 값으로 그대로 쓰인다.
 */
@Entity
@Table(name = "night_snack_application")
public class NightSnackApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 신청 회원. 일반 선착순(GENERAL)은 필수이지만, 사전신청(RESERVED)은 Comit 계정 없이 학번만으로
     * 만들어질 수 있어 {@code null} 일 수 있다(요구사항 4-3).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "night_snack_id", nullable = false)
    private NightSnack nightSnack;

    /**
     * 학번. 사전신청은 학번만 받으므로 필수이고, 일반 선착순은 회원 학번을 복사해 둔다(없으면 null).
     * {@code (night_snack_id, student_number)} 유니크로 사전신청↔일반 교차 중복까지 막는다.
     */
    @Column(name = "student_number", length = 20)
    private String studentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NightSnackApplicationSource source;

    @Column(name = "ticket_token", nullable = false, unique = true, length = 36)
    private String ticketToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NightSnackApplicationStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    protected NightSnackApplication() {
    }

    /**
     * 일반 선착순 신청(GENERAL). 회원이 필수이며, 교차 중복 차단을 위해 회원의 학번을 함께 저장한다.
     */
    public static NightSnackApplication general(Member member, NightSnack nightSnack) {
        if (member == null || nightSnack == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return create(member, nightSnack, member.getStudentNumber(), NightSnackApplicationSource.GENERAL);
    }

    /**
     * 사전신청(RESERVED). 관리자가 학번만 받아 오픈 전에 등록한다. 회원 계정 없이 학번만으로 만들어진다.
     */
    public static NightSnackApplication reserved(NightSnack nightSnack, String studentNumber) {
        if (nightSnack == null || studentNumber == null || studentNumber.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return create(null, nightSnack, studentNumber.trim(), NightSnackApplicationSource.RESERVED);
    }

    private static NightSnackApplication create(
            Member member, NightSnack nightSnack, String studentNumber, NightSnackApplicationSource source) {
        NightSnackApplication application = new NightSnackApplication();
        application.member = member;
        application.nightSnack = nightSnack;
        application.studentNumber = studentNumber;
        application.source = source;
        application.ticketToken = UUID.randomUUID().toString();
        application.status = NightSnackApplicationStatus.PENDING;
        application.createdAt = LocalDateTime.now();
        return application;
    }

    /**
     * QR 스캔 수령 처리. 다음 단계(수령 확인 API)에서 사용한다 — 초안에는 호출처가 없다.
     */
    public void confirm() {
        if (this.status != NightSnackApplicationStatus.PENDING) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        this.status = NightSnackApplicationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public NightSnack getNightSnack() {
        return nightSnack;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public NightSnackApplicationSource getSource() {
        return source;
    }

    public String getTicketToken() {
        return ticketToken;
    }

    public NightSnackApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
}
