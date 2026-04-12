package kr.ac.knu.comit.member.dto;

import kr.ac.knu.comit.member.domain.Member;

public record MemberProfileResponse(
        Long id,
        String nickname,
        String studentNumber,
        boolean studentNumberVisible,
        String profileImageUrl
) {
    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getStudentNumber(),
                member.isStudentNumberVisible(),
                member.getProfileImageUrl()
        );
    }
}
