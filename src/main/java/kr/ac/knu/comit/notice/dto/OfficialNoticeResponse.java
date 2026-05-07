package kr.ac.knu.comit.notice.dto;

import kr.ac.knu.comit.notice.domain.OfficialNotice;

import java.time.LocalDateTime;

public record OfficialNoticeResponse(
        Long id,
        String title,
        String content,
        String author,
        String originalUrl,
        LocalDateTime postedAt,
        String summary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OfficialNoticeResponse from(OfficialNotice notice) {
        return new OfficialNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getAuthor(),
                notice.getOriginalUrl(),
                notice.getPostedAt(),
                notice.getSummary(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
