package kr.ac.knu.comit.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.dto.HotPostListResponse;
import kr.ac.knu.comit.post.dto.HotPostResponse;
import kr.ac.knu.comit.post.dto.PostSummaryResponse;
import kr.ac.knu.comit.post.service.PostService;
import kr.ac.knu.comit.main.dto.MainPageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MainService")
class MainServiceTest {

    @Mock
    private MainPageQueryService mainPageQueryService;

    @Mock
    private PostService postService;

    @InjectMocks
    private MainService mainService;

    @Nested
    @DisplayName("getMainPage")
    class GetMainPage {

        @Test
        @DisplayName("섹션 조회 결과와 인기글을 하나의 메인 응답으로 조합한다")
        void assemblesSectionsAndHotPosts() {
            // given
            // 게시판별 섹션과 인기글 조회 결과를 준비한다.
            MainPageSections sections = mainPageSections(
                    List.of(postSummary(101L, BoardType.QNA, "QNA 최신글")),
                    List.of(postSummary(201L, BoardType.INFO, "INFO 최신글")),
                    List.of(postSummary(301L, BoardType.FREE, "FREE 최신글")),
                    List.of(postSummary(401L, BoardType.NOTICE, "NOTICE 최신글")),
                    List.of(postSummary(501L, BoardType.EVENT, "EVENT 최신글"))
            );
            HotPostListResponse hotPosts = new HotPostListResponse(List.of(
                    hotPost(1, 901L, BoardType.QNA, "인기글")
            ));
            given(mainPageQueryService.getSections(5)).willReturn(sections);
            given(postService.getHotPosts()).willReturn(hotPosts);

            // when
            // 메인 페이지 조합을 실행한다.
            MainPageResponse response = mainService.getMainPage();

            // then
            // 각 섹션과 인기글이 그대로 조합되어 반환되어야 한다.
            assertThat(response.qna()).containsExactly(sections.qna().get(0));
            assertThat(response.info()).containsExactly(sections.info().get(0));
            assertThat(response.free()).containsExactly(sections.free().get(0));
            assertThat(response.notice()).containsExactly(sections.notice().get(0));
            assertThat(response.event()).containsExactly(sections.event().get(0));
            assertThat(response.hotPosts()).containsExactly(hotPosts.posts().get(0));

            then(mainPageQueryService).should().getSections(5);
            then(postService).should().getHotPosts();
            then(mainPageQueryService).shouldHaveNoMoreInteractions();
            then(postService).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("비어 있는 섹션은 빈 배열 그대로 유지한다")
        void keepsEmptySectionsIntact() {
            // given
            // 모든 섹션과 인기글이 비어 있는 조회 결과를 준비한다.
            MainPageSections sections = mainPageSections(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
            given(mainPageQueryService.getSections(5)).willReturn(sections);
            given(postService.getHotPosts()).willReturn(HotPostListResponse.empty());

            // when
            // 메인 페이지 조합을 실행한다.
            MainPageResponse response = mainService.getMainPage();

            // then
            // 어떤 섹션도 null 이 아니고 빈 배열로 유지되어야 한다.
            assertThat(response.qna()).isEmpty();
            assertThat(response.info()).isEmpty();
            assertThat(response.free()).isEmpty();
            assertThat(response.notice()).isEmpty();
            assertThat(response.event()).isEmpty();
            assertThat(response.hotPosts()).isEmpty();
        }
    }

    private MainPageSections mainPageSections(
            List<PostSummaryResponse> qna,
            List<PostSummaryResponse> info,
            List<PostSummaryResponse> free,
            List<PostSummaryResponse> notice,
            List<PostSummaryResponse> event
    ) {
        return new MainPageSections(qna, info, free, notice, event);
    }

    private PostSummaryResponse postSummary(Long id, BoardType boardType, String title) {
        return new PostSummaryResponse(
                id,
                boardType,
                title,
                "본문 미리보기",
                "작성자",
                "https://cdn.example.com/profile-" + id + ".png",
                3,
                2,
                List.of("spring"),
                List.of("https://cdn.example.com/post-" + id + ".png"),
                LocalDateTime.parse("2026-04-01T12:00:00")
        );
    }

    private HotPostResponse hotPost(int rank, Long id, BoardType boardType, String title) {
        return new HotPostResponse(
                rank,
                id,
                boardType,
                title,
                "작성자",
                "https://cdn.example.com/profile-" + id + ".png",
                11,
                4,
                List.of("spring"),
                LocalDateTime.parse("2026-04-01T12:00:00")
        );
    }
}
