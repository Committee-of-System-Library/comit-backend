package kr.ac.knu.comit.auth.port;

/**
 * 외부 인증 공급자가 보장하는 최소 사용자 정보 모델.
 *
 * @apiNote enum 해석은 Comit 내부 정책이므로 raw 값을 그대로 들고 들어온 뒤
 * mapper 계층에서 {@code MemberPrincipal}로 변환한다.
 */
public record ExternalIdentity(
        String ssoSub,
        String name,
        String email,
        String studentNumber,
        String major,
        String userType,
        String role
) {
}
