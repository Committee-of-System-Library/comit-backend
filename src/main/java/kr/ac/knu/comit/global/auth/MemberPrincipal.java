package kr.ac.knu.comit.global.auth;

/**
 * KNU CSE SSO claim으로부터 만든 인증 컨텍스트.
 *
 * @apiNote {@code memberId}는 SSO 사용자를 우리 서비스 회원과 매칭하거나
 * 최초 생성한 뒤 확정되는 로컬 DB 식별자다.
 */
public record MemberPrincipal(
        Long memberId,
        String ssoSub,
        String name,
        String email,
        String studentNumber,
        UserType userType,
        MemberRole role
) {

    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }

    public enum UserType {
        CSE_STUDENT, KNU_OTHER_DEPT, EXTERNAL
    }

    public enum MemberRole {
        ADMIN, STUDENT
    }
}
