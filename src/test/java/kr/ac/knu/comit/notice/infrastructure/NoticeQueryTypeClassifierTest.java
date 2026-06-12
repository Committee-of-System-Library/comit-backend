package kr.ac.knu.comit.notice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import kr.ac.knu.comit.notice.infrastructure.rag.NoticeQueryType;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeQueryTypeClassifier;
import org.junit.jupiter.api.Test;

class NoticeQueryTypeClassifierTest {

    private final NoticeQueryTypeClassifier classifier = new NoticeQueryTypeClassifier();

    @Test
    void classifyOutOfScopeQuestion() {
        assertThat(classifier.maxSources("오늘 기숙사 식단 알려줘")).isZero();
    }

    @Test
    void classifyDetailQuestion() {
        assertThat(classifier.maxSources("석사우수장학금 금액 얼마야?")).isEqualTo(2);
    }

    @Test
    void classifyExactTokenQuestion() {
        assertThat(classifier.classify("Exit 인터뷰 공지 알려줘")).isEqualTo(NoticeQueryType.EXACT_TOKEN_DETAIL);
    }

    @Test
    void classifyDomainLabelQuestion() {
        assertThat(classifier.maxSources("ABEEK 이수 포기 공지 알려줘")).isEqualTo(2);
    }

    @Test
    void classifyLongSingleNoticeQuestion() {
        assertThat(classifier.maxSources("2026학년도 1학기 수강신청 안내 찾아줘")).isEqualTo(2);
    }

    @Test
    void classifyMultiNoticeQuestion() {
        assertThat(classifier.maxSources("장학금 관련 공지사항들 알려줘")).isEqualTo(5);
    }

    @Test
    void classifyDeadlineSearchByCurrentKeyword() {
        assertThat(classifier.classify("현재 모집중인 장학금 있어?")).isEqualTo(NoticeQueryType.DEADLINE_SEARCH);
    }

    @Test
    void classifyDeadlineSearchByThisWeekKeyword() {
        assertThat(classifier.classify("이번 주 마감인 공지 알려줘")).isEqualTo(NoticeQueryType.DEADLINE_SEARCH);
    }

    @Test
    void classifyDeadlineSearchByProgressKeyword() {
        assertThat(classifier.classify("지금 진행중인 공모전 있나요?")).isEqualTo(NoticeQueryType.DEADLINE_SEARCH);
    }

    @Test
    void deadlineSearchReturnsMaxFiveSources() {
        assertThat(classifier.maxSources("현재 신청 가능한 공지 있어?")).isEqualTo(5);
    }
}
