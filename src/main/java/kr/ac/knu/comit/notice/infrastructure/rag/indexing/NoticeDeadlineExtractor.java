package kr.ac.knu.comit.notice.infrastructure.rag.indexing;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NoticeDeadlineExtractor {

    private static final Pattern DEADLINE_ADJACENT = Pattern.compile(
            "(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일\\s*(까지|마감|기한|접수마감|신청마감)"
    );
    private static final Pattern FULL_DATE = Pattern.compile(
            "(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일"
    );

    private NoticeDeadlineExtractor() {
    }

    static LocalDate extract(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        LocalDate deadline = findLatest(DEADLINE_ADJACENT, content);
        if (deadline != null) {
            return deadline;
        }
        return findLatest(FULL_DATE, content);
    }

    private static LocalDate findLatest(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        LocalDate latest = null;
        while (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                LocalDate date = LocalDate.of(year, month, day);
                if (latest == null || date.isAfter(latest)) {
                    latest = date;
                }
            } catch (Exception ignored) {
            }
        }
        return latest;
    }
}
