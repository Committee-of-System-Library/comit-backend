package kr.ac.knu.comit.notice.dto;

public record NoticeSource(
        Long noticeId,
        String title,
        String originalUrl
) {
}
