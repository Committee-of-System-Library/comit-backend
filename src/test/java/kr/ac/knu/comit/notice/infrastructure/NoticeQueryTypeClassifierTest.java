package kr.ac.knu.comit.notice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoticeQueryTypeClassifierTest {

    private final NoticeQueryTypeClassifier classifier = new NoticeQueryTypeClassifier();

    @Test
    void classifyOutOfScopeQuestion() {
        assertThat(classifier.maxSources("오늘 기숙사 식단 알려줘")).isZero();
    }

    @Test
    void classifyDetailQuestion() {
        assertThat(classifier.maxSources("석사우수장학금 금액 얼마야?")).isEqualTo(1);
    }

    @Test
    void classifyExactTokenQuestion() {
        assertThat(classifier.maxSources("Exit 인터뷰 업로드링크 알려줘")).isEqualTo(1);
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
}
