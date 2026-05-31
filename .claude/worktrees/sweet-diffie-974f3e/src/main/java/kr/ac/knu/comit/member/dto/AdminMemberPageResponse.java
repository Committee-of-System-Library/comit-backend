package kr.ac.knu.comit.member.dto;

import java.util.List;
import kr.ac.knu.comit.member.domain.Member;
import org.springframework.data.domain.Page;

public record AdminMemberPageResponse(
        List<AdminMemberSummaryResponse> members,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminMemberPageResponse from(Page<Member> memberPage) {
        List<AdminMemberSummaryResponse> members = memberPage.getContent().stream()
                .map(AdminMemberSummaryResponse::from)
                .toList();
        return new AdminMemberPageResponse(
                members,
                memberPage.getNumber(),
                memberPage.getSize(),
                memberPage.getTotalElements(),
                memberPage.getTotalPages()
        );
    }
}
