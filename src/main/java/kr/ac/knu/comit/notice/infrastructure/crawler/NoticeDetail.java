package kr.ac.knu.comit.notice.infrastructure.crawler;

import java.time.LocalDateTime;

public record NoticeDetail(
        String content,
        LocalDateTime postedAt
) {
}
