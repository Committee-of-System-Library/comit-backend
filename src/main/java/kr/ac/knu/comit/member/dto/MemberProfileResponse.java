package kr.ac.knu.comit.member.dto;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.member.domain.Member;

public record MemberProfileResponse(
        Long id,
        String nickname,
        String studentNumber,
        boolean studentNumberVisible,
        String profileImageUrl,
        String majorTrack,
        MemberPrincipal.MemberRole role
) {
    public static MemberProfileResponse from(Member member, MemberPrincipal.MemberRole role) {
        return new MemberProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getStudentNumber(),
                member.isStudentNumberVisible(),
                member.getProfileImageUrl(),
                member.getMajorTrack(),
                role
        );
    }
}
