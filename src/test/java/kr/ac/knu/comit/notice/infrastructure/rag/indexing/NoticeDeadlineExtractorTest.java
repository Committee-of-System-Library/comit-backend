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
