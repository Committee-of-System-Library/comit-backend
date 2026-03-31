package kr.ac.knu.comit.member.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.MemberErrorCode;

@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ssoSub;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(length = 20)
    private String studentNumber;

    private String majorTrack;

    @Column(nullable = false)
    private boolean studentNumberVisible = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    private LocalDateTime suspendedUntil;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime agreedAt;

    private LocalDateTime deletedAt;

    protected Member() {
    }

    public static Member create(
            String ssoSub,
            String name,
            String phone,
            String nickname,
            String studentNumber,
            String majorTrack,
            LocalDateTime agreedAt
    ) {
        Member member = new Member();
        member.ssoSub = requireText(ssoSub);
        member.name = requireText(name);
        member.phone = requireText(phone);
        member.nickname = normalizeNickname(nickname);
        member.studentNumber = normalizeStudentNumber(studentNumber);
        member.majorTrack = normalizeOptionalText(majorTrack);
        member.studentNumberVisible = true;
        member.createdAt = LocalDateTime.now();
        member.agreedAt = requireAgreedAt(agreedAt);
        return member;
    }

    public void updateNickname(String nickname) {
        this.nickname = normalizeNickname(nickname);
    }

    public void updateStudentNumberVisibility(boolean visible) {
        this.studentNumberVisible = visible;
    }

    public void syncStudentNumber(String studentNumber) {
        this.studentNumber = normalizeStudentNumber(studentNumber);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public Long getId() {
        return id;
    }

    public String getSsoSub() {
        return ssoSub;
    }

    public String getNickname() {
        return nickname;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getMajorTrack() {
        return majorTrack;
    }

    public boolean isStudentNumberVisible() {
        return studentNumberVisible;
    }

    public void suspend(LocalDateTime until) {
        validateSuspendedUntil(until);
        this.status = MemberStatus.SUSPENDED;
        this.suspendedUntil = until;
    }

    public void ban() {
        this.status = MemberStatus.BANNED;
        this.suspendedUntil = null;
    }

    public void activate() {
        this.status = MemberStatus.ACTIVE;
        this.suspendedUntil = null;
    }

    public boolean isSuspended() {
        if (status != MemberStatus.SUSPENDED) return false;
        if (suspendedUntil == null) return true;
        return LocalDateTime.now().isBefore(suspendedUntil);
    }

    public boolean isBanned() {
        return status == MemberStatus.BANNED;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public LocalDateTime getSuspendedUntil() {
        return suspendedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAgreedAt() {
        return agreedAt;
    }

    private static String normalizeNickname(String nickname) {
        String normalized = normalizeOptionalText(nickname);
        if (normalized == null || normalized.length() > 15) {
            throw new BusinessException(MemberErrorCode.INVALID_NICKNAME);
        }
        return normalized;
    }

    private static String requireText(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private static String normalizeStudentNumber(String studentNumber) {
        return normalizeOptionalText(studentNumber);
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static LocalDateTime requireAgreedAt(LocalDateTime agreedAt) {
        if (agreedAt == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return agreedAt;
    }

    private static void validateSuspendedUntil(LocalDateTime suspendedUntil) {
        if (suspendedUntil != null && !suspendedUntil.isAfter(LocalDateTime.now())) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }
}
