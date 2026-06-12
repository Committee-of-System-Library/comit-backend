package kr.ac.knu.comit.notice.infrastructure.rag;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class NoticeQueryTypeClassifier {

    private static final List<String> OUT_OF_SCOPE_KEYWORDS = List.of(
            "식단", "학식", "날씨", "버스", "셔틀", "지도", "위치"
    );
    private static final List<String> MULTI_NOTICE_KEYWORDS = List.of(
            "공지사항들", "공지들", "문서들", "관련 공지", "관련 문서", "목록", "모아", "전체", "여러",
            "요약", "정리해", "최근 공지", "최신 공지"
    );
    private static final List<String> DEADLINE_SEARCH_KEYWORDS = List.of(
            "이번 주", "이번 달", "오늘까지", "마감 임박", "임박", "다가오는", "곧 마감",
            "현재", "지금", "모집중", "진행중", "신청 가능"
    );
    private static final List<String> DETAIL_KEYWORDS = List.of(
            "얼마", "금액", "언제", "기한", "마감", "링크", "제출서류", "대상", "방법", "어디", "이메일", "신청기간",
            "양식", "신청서", "가능한지", "신청 가능", "해당되는지", "복수전공"
    );
    private static final List<String> EXACT_TOKEN_KEYWORDS = List.of(
            "exit", "dsac", "5-step", "1:1"
    );
    private static final List<String> DOMAIN_LABEL_KEYWORDS = List.of(
            "abeek", "글솝", "플솝", "인컴", "심컴"
    );
    private static final List<String> LONG_NOTICE_SINGLE_KEYWORDS = List.of(
            "수강신청 안내"
    );
    private static final List<String> REPEATED_TITLE_KEYWORDS = List.of(
            "학위논문 제출", "수강신청", "수강변경"
    );

    public NoticeQueryType classify(String query) {
        String normalized = normalize(query);

        if (containsAny(normalized, OUT_OF_SCOPE_KEYWORDS)) {
            return NoticeQueryType.OUT_OF_SCOPE;
        }
        if (containsAny(normalized, MULTI_NOTICE_KEYWORDS)) {
            return NoticeQueryType.CATEGORY_MULTI_NOTICE_SEARCH;
        }
        if (containsAny(normalized, DEADLINE_SEARCH_KEYWORDS)) {
            return NoticeQueryType.DEADLINE_SEARCH;
        }
        if (containsAny(normalized, DETAIL_KEYWORDS)) {
            return NoticeQueryType.DETAIL_ANSWER;
        }
        if (containsAny(normalized, EXACT_TOKEN_KEYWORDS)) {
            return NoticeQueryType.EXACT_TOKEN_DETAIL;
        }
        if (containsAny(normalized, DOMAIN_LABEL_KEYWORDS)) {
            return NoticeQueryType.DOMAIN_LABEL_SINGLE_NOTICE_SEARCH;
        }
        if (containsAny(normalized, LONG_NOTICE_SINGLE_KEYWORDS)) {
            return NoticeQueryType.LONG_NOTICE_SINGLE_SEARCH;
        }
        if (containsAny(normalized, REPEATED_TITLE_KEYWORDS)) {
            return NoticeQueryType.REPEATED_TITLE_SEARCH;
        }
        if (isNoticeSearch(normalized)) {
            return NoticeQueryType.SINGLE_NOTICE_SEARCH;
        }
        return NoticeQueryType.DEFAULT;
    }

    public int maxSources(String query) {
        return classify(query).maxSources();
    }

    private boolean isNoticeSearch(String query) {
        return query.contains("공지") || query.contains("안내") || query.contains("찾아");
    }

    private boolean containsAny(String query, List<String> keywords) {
        return keywords.stream().anyMatch(query::contains);
    }

    private String normalize(String query) {
        if (query == null) {
            return "";
        }
        return query.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
