package kr.ac.knu.comit.notice.infrastructure.rag.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NoticeDeadlineExtractorTest {

    @Test
    void extractDeadlineWithUntilKeyword() {
        String content = "신청 기간: 2026년 5월 31일까지";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    void extractDeadlineWithDeadlineKeyword() {
        String content = "접수 마감: 2026년 6월 15일 마감";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void extractDeadlineWithApplicationDeadlineKeyword() {
        String content = "2026년 7월 1일 신청마감입니다.";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void extractDelimitedDeadlineWithUntilKeyword() {
        String content = "신청은 2026.06.10까지 가능합니다.";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void extractDeadlineWhenKeywordComesBeforeDate() {
        String content = "접수 마감: 2026년 6월 10일 18:00";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void extractDeadlineWithDocumentSubmissionKeyword() {
        String content = "서류 제출 기한: 2026년 6월 10일";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void extractEndDateFromDelimitedApplicationPeriod() {
        String content = "신청기간: 2026.6.1 ~ 2026.6.10";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void extractEndDateFromYearlessApplicationPeriod() {
        String content = "신청기간: 6.1 ~ 6.10";
        assertThat(NoticeDeadlineExtractor.extract(content))
                .isEqualTo(LocalDate.of(LocalDate.now().getYear(), 6, 10));
    }

    @Test
    void inferYearFromContextForYearlessApplicationPeriod() {
        String content = "2026학년도 신청기간: 6.1 ~ 6.10";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void preferAdjacentKeywordOverLatestDate() {
        String content = "행사 일정: 2026년 8월 1일. 신청 마감: 2026년 5월 31일까지";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    void fallbackToLatestDateWhenNoAdjacentKeyword() {
        String content = "1차: 2026년 3월 10일, 2차: 2026년 4월 20일, 최종: 2026년 5월 5일";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 5, 5));
    }

    @Test
    void returnNullWhenNoDateFound() {
        assertThat(NoticeDeadlineExtractor.extract("날짜 정보가 없는 공지입니다.")).isNull();
    }

    @Test
    void returnNullForOpenEndedApplicationWithoutFixedDeadline() {
        String content = "상시 모집합니다. 교육일은 2026년 6월 10일입니다.";
        assertThat(NoticeDeadlineExtractor.extract(content)).isNull();
    }

    @Test
    void returnNullForFirstComeFirstServedWithoutFixedDeadline() {
        String content = "선착순 접수, 마감 시까지 모집합니다. 행사일: 2026년 6월 10일";
        assertThat(NoticeDeadlineExtractor.extract(content)).isNull();
    }

    @Test
    void extractFixedDeadlineEvenWhenFirstComeFirstServedExists() {
        String content = "선착순 접수이며 2026년 6월 10일까지 신청 가능합니다.";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void returnNullForNullContent() {
        assertThat(NoticeDeadlineExtractor.extract(null)).isNull();
    }

    @Test
    void returnNullForBlankContent() {
        assertThat(NoticeDeadlineExtractor.extract("   ")).isNull();
    }

    @Test
    void extractLatestWhenMultipleAdjacentKeywords() {
        String content = "1차 마감: 2026년 4월 10일까지, 최종 마감: 2026년 5월 20일까지";
        assertThat(NoticeDeadlineExtractor.extract(content)).isEqualTo(LocalDate.of(2026, 5, 20));
    }
}
