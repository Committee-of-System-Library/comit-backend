package kr.ac.knu.comit.post.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.fixture.PostFixture;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.service.ContentPreviewGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("PostCursorPageResponse")
class PostCursorPageResponseTest {

    private static final ContentPreviewGenerator PREVIEW = new ContentPreviewGenerator(2);

    @Test
    @DisplayName("한 건을 더 받아오면 현재 페이지 크기로 잘라서 다음 커서를 만든다")
    void trimsOverFetchedPostsAndBuildsNextCursor() {
        // given
        List<Post> fetchedPosts = List.of(
                PostFixture.post(30L),
                PostFixture.post(29L),
                PostFixture.post(28L)
        );

        // when
        PostCursorPageResponse response = PostCursorPageResponse.of(
                fetchedPosts,
                2,
                Map.of(30L, 3, 29L, 1),
                Map.of(),
                PREVIEW::generate
        );

        // then
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursorId()).isEqualTo(29L);
        assertThat(response.posts()).hasSize(2);
        assertThat(response.posts().get(0).commentCount()).isEqualTo(3);
        assertThat(response.posts().get(1).commentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("요청 크기만큼만 있으면 마지막 페이지로 판단한다")
    void marksLastPageWhenNoExtraRowExists() {
        // given
        List<Post> fetchedPosts = List.of(
                PostFixture.post(30L),
                PostFixture.post(29L)
        );

        // when
        PostCursorPageResponse response = PostCursorPageResponse.of(
                fetchedPosts,
                2,
                Map.of(),
                Map.of(),
                PREVIEW::generate
        );

        // then
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursorId()).isNull();
        assertThat(response.posts()).hasSize(2);
    }

    @Test
    @DisplayName("본문이 2줄을 초과하면 첫 2줄만 보여주고 ...을 붙인다")
    void truncatesPreviewAfterMaxLines() {
        // given
        Post post = Post.create(
                MemberFixture.member(99L, "post-writer"),
                BoardType.FREE,
                "preview-title",
                "1절\n동해 물과 백두산이 마르고 닳도록\n하느님이 보우하사 우리나라 만세.\n(후렴) 무궁화 삼천리 화려 강산",
                List.of(),
                List.of()
        );
        ReflectionTestUtils.setField(post, "id", 1L);

        // when
        PostCursorPageResponse response = PostCursorPageResponse.of(
                List.of(post),
                1,
                Map.of(),
                Map.of(),
                PREVIEW::generate
        );

        // then
        assertThat(response.posts().getFirst().contentPreview())
                .isEqualTo("1절 동해 물과 백두산이 마르고 닳도록...");
    }

    @Test
    @DisplayName("본문이 2줄 이하이면 ...없이 그대로 반환한다")
    void doesNotTruncateWhenWithinMaxLines() {
        // given
        Post post = Post.create(
                MemberFixture.member(99L, "post-writer"),
                BoardType.FREE,
                "preview-title",
                "첫 번째 줄입니다.\n두 번째 줄입니다.",
                List.of(),
                List.of()
        );
        ReflectionTestUtils.setField(post, "id", 2L);

        // when
        PostCursorPageResponse response = PostCursorPageResponse.of(
                List.of(post),
                1,
                Map.of(),
                Map.of(),
                PREVIEW::generate
        );

        // then
        assertThat(response.posts().getFirst().contentPreview())
                .isEqualTo("첫 번째 줄입니다. 두 번째 줄입니다.");
    }
}
