package kr.ac.knu.comit.post.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import kr.ac.knu.comit.member.domain.Member;
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
        List<Post> fetchedPosts = List.of(
                post(30L, "third"),
                post(29L, "second"),
                post(28L, "first")
        );

        PostCursorPageResponse response = PostCursorPageResponse.of(
                fetchedPosts,
                2,
                Map.of(30L, 3, 29L, 1)
        );

        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursorId()).isEqualTo(29L);
        assertThat(response.posts()).hasSize(2);
        assertThat(response.posts().get(0).commentCount()).isEqualTo(3);
        assertThat(response.posts().get(1).commentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("요청 크기만큼만 있으면 마지막 페이지로 판단한다")
    void marksLastPageWhenNoExtraRowExists() {
        List<Post> fetchedPosts = List.of(
                post(30L, "third"),
                post(29L, "second")
        );

        PostCursorPageResponse response = PostCursorPageResponse.of(
                fetchedPosts,
                2,
                Map.of()
        );

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursorId()).isNull();
        assertThat(response.posts()).hasSize(2);
    }

    private Post post(Long id, String suffix) {
        Member author = Member.create("sso-" + suffix, "writer-" + suffix, "20230001");
        Post post = Post.create(author, BoardType.QNA, "title-" + suffix, "content-" + suffix, List.of());
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }
}
