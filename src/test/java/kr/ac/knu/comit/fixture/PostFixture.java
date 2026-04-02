package kr.ac.knu.comit.fixture;

import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

public class PostFixture {

    public static Post post() {
        return post(1L);
    }

    public static Post post(Long id) {
        return post(id, 0);
    }

    public static Post post(Long id, int viewCount) {
        Post post = Post.create(MemberFixture.member(99L, "post-writer"), BoardType.QNA, "title-" + id, "content-" + id, List.of(), List.of());
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "viewCount", viewCount);
        return post;
    }
}
