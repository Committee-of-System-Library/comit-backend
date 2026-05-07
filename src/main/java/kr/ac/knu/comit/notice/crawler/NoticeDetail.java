package kr.ac.knu.comit.notice.crawler;

import java.time.LocalDateTime;

public record NoticeDetail(
        String content,
        LocalDateTime postedAt
) {
}
