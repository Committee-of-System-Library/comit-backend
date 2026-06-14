package kr.ac.knu.comit.inquiry.dto;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knu.comit.inquiry.domain.Inquiry;
import org.springframework.data.domain.Page;

public record AdminInquiryPageResponse(
        List<AdminInquirySummary> inquiries,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static AdminInquiryPageResponse from(Page<Inquiry> inquiryPage) {
        return new AdminInquiryPageResponse(
                inquiryPage.getContent().stream().map(AdminInquirySummary::from).toList(),
                inquiryPage.getNumber(),
                inquiryPage.getSize(),
                inquiryPage.getTotalElements(),
                inquiryPage.getTotalPages()
        );
    }

    public record AdminInquirySummary(
            Long id,
            String title,
            String content,
            String memberNickname,
            LocalDateTime createdAt
    ) {

        public static AdminInquirySummary from(Inquiry inquiry) {
            return new AdminInquirySummary(
                    inquiry.getId(),
                    inquiry.getTitle(),
                    inquiry.getContent(),
                    inquiry.getMember().getDisplayNickname(),
                    inquiry.getCreatedAt()
            );
        }
    }
}
