package kr.ac.knu.comit.auth.dto;

import kr.ac.knu.comit.global.auth.MemberPrincipal;

public record SsoClaims(
        String subject,
        String name,
        String email,
        String studentNumber,
        MemberPrincipal.UserType userType,
        MemberPrincipal.MemberRole role
) {
}
