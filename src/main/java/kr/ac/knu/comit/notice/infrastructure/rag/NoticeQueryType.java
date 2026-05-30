package kr.ac.knu.comit.notice.infrastructure.rag;

enum NoticeQueryType {

    OUT_OF_SCOPE(0),
    DETAIL_ANSWER(1),
    SINGLE_NOTICE_SEARCH(1),
    EXACT_TOKEN_DETAIL(2),
    DOMAIN_LABEL_SINGLE_NOTICE_SEARCH(2),
    LONG_NOTICE_SINGLE_SEARCH(2),
    REPEATED_TITLE_SEARCH(3),
    CATEGORY_MULTI_NOTICE_SEARCH(5),
    DEFAULT(3);

    private final int maxSources;

    NoticeQueryType(int maxSources) {
        this.maxSources = maxSources;
    }

    int maxSources() {
        return maxSources;
    }
}
