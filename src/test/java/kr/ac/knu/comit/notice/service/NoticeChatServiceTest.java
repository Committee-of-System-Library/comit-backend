package kr.ac.knu.comit.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kr.ac.knu.comit.notice.domain.OfficialNotice;
import kr.ac.knu.comit.notice.domain.OfficialNoticeRepository;
import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeRagPipeline;
import kr.ac.knu.comit.notice.infrastructure.rag.config.NoticeRagProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NoticeChatServiceTest {

    @Mock
    private NoticeRagPipeline noticeRagPipeline;

    @Mock
    private OfficialNoticeRepository officialNoticeRepository;

    private ExecutorService executorService;
    private NoticeChatService noticeChatService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newSingleThreadExecutor();
        noticeChatService = new NoticeChatService(
                noticeRagPipeline,
                officialNoticeRepository,
                executorService,
                noticeRagProperties()
        );
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void answerLatestNoticeQueryFromDatabaseWithoutRag() {
        // given
        OfficialNotice first = notice(1L, "최신 공지 1", LocalDateTime.of(2026, 6, 5, 9, 0), "첫 번째 요약");
        OfficialNotice second = notice(2L, "최신 공지 2", LocalDateTime.of(2026, 6, 4, 9, 0), null);
        given(officialNoticeRepository.findFirstPage(PageRequest.of(0, 5)))
                .willReturn(List.of(first, second));

        // when
        NoticeChatResponse response = noticeChatService.chat("최신 공지 5개 요약해줘").join();

        // then
        assertThat(response.answer())
                .contains("최신 공지 2개입니다.")
                .contains("[2026-06-05] 최신 공지 1 - 첫 번째 요약")
                .contains("[2026-06-04] 최신 공지 2");
        assertThat(response.sources())
                .extracting("noticeId")
                .containsExactly(1L, 2L);
        then(noticeRagPipeline).should(never()).chat(anyString());
    }

    private OfficialNotice notice(Long id, String title, LocalDateTime postedAt, String summary) {
        OfficialNotice notice = OfficialNotice.create(
                String.valueOf(id),
                title,
                "공지사항 본문입니다.",
                "학사지원팀",
                "https://cse.knu.ac.kr/notice/" + id,
                postedAt,
                summary
        );
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }

    private NoticeRagProperties noticeRagProperties() {
        return new NoticeRagProperties(
                10,
                0.7,
                "gpt-4.1-nano",
                "gpt-4.1-nano",
                "gpt-4.1-nano",
                "gpt-4o-mini",
                "gpt-4o",
                "gpt-4.1-nano",
                Duration.ofSeconds(10),
                1,
                1,
                10
        );
    }
}
