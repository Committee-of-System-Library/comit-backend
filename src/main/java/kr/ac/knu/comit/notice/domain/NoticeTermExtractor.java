package kr.ac.knu.comit.notice.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoticeTermExtractor {

    private static final Pattern ACADEMIC_YEAR_PATTERN = Pattern.compile("(\\d{4})학년도");
    private static final Pattern SEMESTER_PATTERN = Pattern.compile("([12])학기");

    private NoticeTermExtractor() {
    }

    public static Integer extractAcademicYear(String title) {
        if (title == null) {
            return null;
        }
        Matcher matcher = ACADEMIC_YEAR_PATTERN.matcher(title);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    public static Integer extractSemester(String title) {
        if (title == null) {
            return null;
        }
        Matcher matcher = SEMESTER_PATTERN.matcher(title);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }
}
