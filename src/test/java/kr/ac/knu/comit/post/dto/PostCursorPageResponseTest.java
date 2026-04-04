package kr.ac.knu.comit.post.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import kr.ac.knu.comit.fixture.PostFixture;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("PostCursorPageResponse")
class PostCursorPageResponseTest {

    @Test
    @DisplayName("한 건을 더 받아오면 현재 페이지 크기로 잘라서 다음 커서를 만든다")
    void trimsOverFetchedPostsAndBuildsNextCursor() {
        // given
        // 요청 크기보다 한 건 더 조회된 게시글 목록을 준비한다.
        List<Post> fetchedPosts = List.of(
                PostFixture.post(30L),
                PostFixture.post(29L),
                PostFixture.post(28L)
        );

        // when
        // cursor 페이지 응답으로 변환한다.
        PostCursorPageResponse response = PostCursorPageResponse.of(
                fetchedPosts,
                2,
                Map.of(30L, 3, 29L, 1),
                Map.of()
        );

        // then
        // visible row만 남기고 다음 커서와 hasNext가 올바르게 계산되는지 확인한다.
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
        // 요청 크기와 정확히 같은 게시글 목록을 준비한다.
        List<Post> fetchedPosts = List.of(
                PostFixture.post(30L),
                PostFixture.post(29L)
        );

        // when
        // cursor 페이지 응답으로 변환한다.
        PostCursorPageResponse response = PostCursorPageResponse.of(
                fetchedPosts,
                2,
                Map.of(),
                Map.of()
        );

        // then
        // 추가 행이 없으면 마지막 페이지로 판단해야 한다.
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursorId()).isNull();
        assertThat(response.posts()).hasSize(2);
    }

    @Test
    @DisplayName("게시글 본문 미리보기는 공백을 정리하고 80자를 넘으면 ...을 붙인다")
    void buildsNormalizedContentPreview() {
        // given
        Post post = Post.create(
                MemberFixture.member(99L, "post-writer"),
                BoardType.FREE,
                "preview-title",
                "  첫 줄입니다.\n\n두 번째 줄에는   공백이 많고\t세 번째 문장까지 이어집니다. "
                        + "이 문장은 길이를 초과하도록 조금 더 이어서 작성합니다. "
                        + "마지막 문장도 덧붙여서 미리보기 잘림을 확인합니다.  ",
                List.of(),
                List.of()
        );
        ReflectionTestUtils.setField(post, "id", 1L);

        // when
        PostCursorPageResponse response = PostCursorPageResponse.of(
                List.of(post),
                1,
                Map.of(),
                Map.of()
        );

        // then
        assertThat(response.posts().getFirst().contentPreview())
                .isEqualTo("첫 줄입니다. 두 번째 줄에는 공백이 많고 세 번째 문장까지 이어집니다. 이 문장은 길이를 초과하도록 조금 더 이어서 작성합니다. 마지막 문장도...");
    }
}
