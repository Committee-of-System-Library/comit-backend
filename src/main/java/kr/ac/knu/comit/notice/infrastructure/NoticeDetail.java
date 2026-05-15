package kr.ac.knu.comit.notice.infrastructure;

import java.time.LocalDateTime;

public record NoticeDetail(
        String content,
        LocalDateTime postedAt
) {
}
