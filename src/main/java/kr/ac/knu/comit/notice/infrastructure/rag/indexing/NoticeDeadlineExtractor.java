package kr.ac.knu.comit.notice.infrastructure.rag.indexing;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NoticeDeadlineExtractor {

    private static final String DATE_TOKEN = "(?:(?:\\d{4}\\s*년\\s*)?\\d{1,2}\\s*월\\s*\\d{1,2}\\s*일?"
            + "|(?:\\d{4}\\s*[./-]\\s*)?\\d{1,2}\\s*[./-]\\s*\\d{1,2})";
    private static final String CAPTURED_DATE_TOKEN = "(" + DATE_TOKEN + ")";
    private static final String OPTIONAL_DATE_SUFFIX = "(?:\\s*\\([^)]*\\))?";

    private static final Pattern KOREAN_DATE = Pattern.compile(
            "(?:(\\d{4})\\s*년\\s*)?(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일?"
    );
    private static final Pattern DELIMITED_DATE = Pattern.compile(
            "(?:(\\d{4})\\s*[./-]\\s*)?(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})"
    );
    private static final Pattern CONTEXT_YEAR = Pattern.compile("(19\\d{2}|20\\d{2})");

    private static final Pattern DATE_BEFORE_DEADLINE_KEYWORD = Pattern.compile(
            CAPTURED_DATE_TOKEN + OPTIONAL_DATE_SUFFIX
                    + "\\s*(?:까지|접수\\s*마감|신청\\s*마감|제출\\s*마감|마감|기한)"
    );
    private static final Pattern DEADLINE_KEYWORD_BEFORE_DATE = Pattern.compile(
            "(?:접수\\s*마감|신청\\s*마감|제출\\s*마감|서류\\s*제출\\s*기한|"
                    + "접수\\s*기한|신청\\s*기한|제출\\s*기한|마감|기한)"
                    + "[^\\n\\r.!?。]{0,40}?" + CAPTURED_DATE_TOKEN
    );
    private static final Pattern DATE_RANGE = Pattern.compile(
            CAPTURED_DATE_TOKEN + OPTIONAL_DATE_SUFFIX
                    + "\\s*(?:~|∼|～|부터|에서|-)\\s*"
                    + CAPTURED_DATE_TOKEN + OPTIONAL_DATE_SUFFIX
    );
    private static final Pattern APPLICATION_RANGE_KEYWORD = Pattern.compile(
            "(?:신청|접수|모집|제출|등록)\\s*(?:기간|일정)"
    );
    private static final Pattern OPEN_ENDED_DEADLINE = Pattern.compile(
            "상시|선착순|별도\\s*공지|마감\\s*시까지|예산\\s*소진\\s*시|충원\\s*시까지"
    );
    private static final Pattern ANY_DATE = Pattern.compile(CAPTURED_DATE_TOKEN);

    private static final int RANGE_KEYWORD_LOOKBEHIND_CHARS = 30;

    private NoticeDeadlineExtractor() {
    }

    static LocalDate extract(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Integer contextYear = findContextYear(content);
        LocalDate deadline = latest(
                findLatestDate(DATE_BEFORE_DEADLINE_KEYWORD, content, contextYear),
                findLatestDate(DEADLINE_KEYWORD_BEFORE_DATE, content, contextYear)
        );
        if (deadline != null) {
            return deadline;
        }

        deadline = findLatestRangeEnd(content, contextYear, true);
        if (deadline != null) {
            return deadline;
        }

        if (OPEN_ENDED_DEADLINE.matcher(content).find()) {
            return null;
        }

        deadline = findLatestRangeEnd(content, contextYear, false);
        if (deadline != null) {
            return deadline;
        }

        return findLatestDate(ANY_DATE, content, contextYear);
    }

    private static LocalDate findLatestDate(Pattern pattern, String content, Integer contextYear) {
        Matcher matcher = pattern.matcher(content);
        LocalDate latest = null;
        while (matcher.find()) {
            LocalDate date = parseDate(matcher.group(1), contextYear);
            if (date != null && (latest == null || date.isAfter(latest))) {
                latest = date;
            }
        }
        return latest;
    }

    private static LocalDate findLatestRangeEnd(String content, Integer contextYear, boolean requireKeyword) {
        Matcher matcher = DATE_RANGE.matcher(content);
        LocalDate latest = null;
        while (matcher.find()) {
            if (requireKeyword && !hasApplicationRangeKeywordNear(content, matcher.start())) {
                continue;
            }

            DateParts startParts = parseDateParts(matcher.group(1));
            LocalDate start = toDate(startParts, contextYear);
            if (start == null) {
                continue;
            }

            DateParts endParts = parseDateParts(matcher.group(2));
            LocalDate end = toDate(endParts, start.getYear());
            if (end == null) {
                continue;
            }
            if (endParts.year() == null && end.isBefore(start)) {
                end = end.plusYears(1);
            }

            if (latest == null || end.isAfter(latest)) {
                latest = end;
            }
        }
        return latest;
    }

    private static boolean hasApplicationRangeKeywordNear(String content, int rangeStart) {
        int start = Math.max(0, rangeStart - RANGE_KEYWORD_LOOKBEHIND_CHARS);
        String prefix = content.substring(start, rangeStart);
        return APPLICATION_RANGE_KEYWORD.matcher(prefix).find();
    }

    private static LocalDate parseDate(String token, Integer defaultYear) {
        return toDate(parseDateParts(token), defaultYear);
    }

    private static DateParts parseDateParts(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String normalizedToken = token.strip();
        Matcher koreanMatcher = KOREAN_DATE.matcher(normalizedToken);
        if (koreanMatcher.matches()) {
            return new DateParts(parseYear(koreanMatcher.group(1)),
                    Integer.parseInt(koreanMatcher.group(2)),
                    Integer.parseInt(koreanMatcher.group(3)));
        }

        Matcher delimitedMatcher = DELIMITED_DATE.matcher(normalizedToken);
        if (delimitedMatcher.matches()) {
            return new DateParts(parseYear(delimitedMatcher.group(1)),
                    Integer.parseInt(delimitedMatcher.group(2)),
                    Integer.parseInt(delimitedMatcher.group(3)));
        }

        return null;
    }

    private static LocalDate toDate(DateParts parts, Integer defaultYear) {
        if (parts == null) {
            return null;
        }

        int year = parts.year() != null ? parts.year() : defaultYearOrCurrent(defaultYear);
        try {
            return LocalDate.of(year, parts.month(), parts.day());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer findContextYear(String content) {
        Matcher matcher = CONTEXT_YEAR.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static Integer parseYear(String value) {
        return value != null ? Integer.parseInt(value) : null;
    }

    private static int defaultYearOrCurrent(Integer defaultYear) {
        return defaultYear != null ? defaultYear : LocalDate.now().getYear();
    }

    private static LocalDate latest(LocalDate left, LocalDate right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private record DateParts(Integer year, int month, int day) {
    }
}
