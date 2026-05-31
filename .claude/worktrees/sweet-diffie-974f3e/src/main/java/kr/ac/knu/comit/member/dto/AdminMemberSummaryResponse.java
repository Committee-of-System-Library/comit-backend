package kr.ac.knu.comit.member.dto;

import java.time.LocalDateTime;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberStatus;

public record AdminMemberSummaryResponse(
        Long id,
        String nickname,
        String studentNumber,
        MemberStatus status,
        LocalDateTime suspendedUntil,
        LocalDateTime createdAt
) {
    public static AdminMemberSummaryResponse from(Member member) {
        return new AdminMemberSummaryResponse(
                member.getId(),
                member.getNickname(),
                member.getStudentNumber(),
                member.getStatus(),
                member.getSuspendedUntil(),
                member.getCreatedAt()
        );
    }
}
