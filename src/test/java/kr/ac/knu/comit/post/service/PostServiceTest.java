package kr.ac.knu.comit.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.PostErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostDailyVisitorRepository;
import kr.ac.knu.comit.post.domain.PostLikeRepository;
import kr.ac.knu.comit.post.domain.PostRepository;
import kr.ac.knu.comit.post.dto.PostDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostDailyVisitorRepository postDailyVisitorRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private CommentQueryService commentQueryService;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("상세 조회 시 조회수를 증가시키고 최신 게시글로 응답한다")
    void returnsReloadedPostAfterIncrementingViewCount() {
        Post initialPost = post(10L, 7);
        Post reloadedPost = post(10L, 8);
        given(postRepository.findActiveById(10L))
                .willReturn(Optional.of(initialPost))
                .willReturn(Optional.of(reloadedPost));
        given(postLikeRepository.existsByPostIdAndMemberId(10L, 1L)).willReturn(true);

        PostDetailResponse response = postService.getPost(10L, 1L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.viewCount()).isEqualTo(8);
        assertThat(response.likedByMe()).isTrue();

        InOrder inOrder = org.mockito.Mockito.inOrder(
                postRepository, postDailyVisitorRepository, postLikeRepository);
        inOrder.verify(postRepository).findActiveById(10L);
        inOrder.verify(postRepository).incrementViewCount(10L);
        inOrder.verify(postRepository).findActiveById(10L);
        inOrder.verify(postDailyVisitorRepository).insertIgnore(eq(10L), eq(1L), any(LocalDate.class));
        inOrder.verify(postLikeRepository).existsByPostIdAndMemberId(10L, 1L);
    }

    @Test
    @DisplayName("게시글이 없으면 조회수와 방문자 기록을 남기지 않는다")
    void doesNotRecordViewWhenPostIsMissing() {
        given(postRepository.findActiveById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.POST_NOT_FOUND);

        then(postRepository).should().findActiveById(10L);
        then(postRepository).should(never()).incrementViewCount(anyLong());
        then(postRepository).shouldHaveNoMoreInteractions();
        then(postDailyVisitorRepository).shouldHaveNoInteractions();
        then(postLikeRepository).shouldHaveNoInteractions();
    }

    private Post post(Long id, int viewCount) {
        Member author = Member.create("sso-" + id, "writer-" + id, "20230001");
        ReflectionTestUtils.setField(author, "id", 1L);

        Post post = Post.create(author, BoardType.QNA, "title-" + id, "content-" + id, List.of());
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "viewCount", viewCount);
        return post;
    }
}
