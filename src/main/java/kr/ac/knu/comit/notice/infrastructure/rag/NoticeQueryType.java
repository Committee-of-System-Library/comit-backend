package kr.ac.knu.comit.notice.infrastructure.rag;

public enum NoticeQueryType {

    OUT_OF_SCOPE(0, AnswerTier.NANO),
    CATEGORY_MULTI_NOTICE_SEARCH(5, AnswerTier.NANO),
    DEADLINE_SEARCH(5, AnswerTier.MINI),
    DETAIL_ANSWER(2, AnswerTier.MINI),
    SINGLE_NOTICE_SEARCH(1, AnswerTier.MINI),
    EXACT_TOKEN_DETAIL(2, AnswerTier.MINI),
    DOMAIN_LABEL_SINGLE_NOTICE_SEARCH(2, AnswerTier.MINI),
    LONG_NOTICE_SINGLE_SEARCH(2, AnswerTier.MINI),
    REPEATED_TITLE_SEARCH(3, AnswerTier.MINI),
    DEFAULT(3, AnswerTier.MINI);

    private final int maxSources;
    private final AnswerTier answerTier;

    NoticeQueryType(int maxSources, AnswerTier answerTier) {
        this.maxSources = maxSources;
        this.answerTier = answerTier;
    }

    public int maxSources() {
        return maxSources;
    }

    public AnswerTier answerTier() {
        return answerTier;
    }
}
