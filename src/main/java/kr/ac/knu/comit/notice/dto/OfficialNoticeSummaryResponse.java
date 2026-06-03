package kr.ac.knu.comit.notice.dto;

import kr.ac.knu.comit.notice.domain.OfficialNotice;

import java.time.LocalDateTime;

public record OfficialNoticeSummaryResponse(
        Long id,
        String title,
        String author,
        String originalUrl,
        LocalDateTime postedAt,
        LocalDateTime createdAt
) {
    public static OfficialNoticeSummaryResponse from(OfficialNotice notice) {
        return new OfficialNoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getAuthor(),
                notice.getOriginalUrl(),
                notice.getPostedAt(),
                notice.getCreatedAt()
        );
    }
}
