package kr.ac.knu.comit.notice.infra;

import java.time.LocalDateTime;

public record NoticeDetail(
        String content,
        LocalDateTime postedAt
) {
}
