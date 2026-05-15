package kr.ac.knu.comit.notice.infrastructure;

import java.time.LocalDate;

public record NoticeListItem(
        String wrId,
        String title,
        String author,
        String originalUrl,
        LocalDate postedDate
) {
}
