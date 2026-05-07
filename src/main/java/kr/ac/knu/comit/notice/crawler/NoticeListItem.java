package kr.ac.knu.comit.notice.crawler;

import java.time.LocalDate;

public record NoticeListItem(
        String wrId,
        String title,
        String author,
        String originalUrl,
        LocalDate postedDate
) {
}
