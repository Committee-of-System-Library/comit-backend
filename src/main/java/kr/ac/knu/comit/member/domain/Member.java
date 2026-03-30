package kr.ac.knu.comit.member.domain;

import jakarta.persistence.*;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.MemberErrorCode;

import java.time.LocalDateTime;

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

    @Column(length = 20)
    private String studentNumber;

    @Column(nullable = false)
    private boolean studentNumberVisible = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    private LocalDateTime suspendedUntil;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    protected Member() {
    }

    /**
     * SSO 최초 인증 성공 시 회원을 생성한다.
     *
     * @apiNote 초기 닉네임은 SSO 표시 이름을 사용하며, 이후 사용자가 변경할 수 있다.
     */
    public static Member create(String ssoSub, String initialNickname, String studentNumber) {
        validateNickname(initialNickname);
        Member member = new Member();
        member.ssoSub = ssoSub;
        member.nickname = initialNickname;
        member.studentNumber = normalizeStudentNumber(studentNumber);
        member.studentNumberVisible = true;
        member.createdAt = LocalDateTime.now();
        return member;
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
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

    public String getStudentNumber() {
        return studentNumber;
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

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank() || nickname.length() > 15) {
            throw new BusinessException(MemberErrorCode.INVALID_NICKNAME);
        }
    }

    private static String normalizeStudentNumber(String studentNumber) {
        if (studentNumber == null) {
            return null;
        }
        String normalized = studentNumber.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void validateSuspendedUntil(LocalDateTime suspendedUntil) {
        if (suspendedUntil != null && !suspendedUntil.isAfter(LocalDateTime.now())) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }
}
