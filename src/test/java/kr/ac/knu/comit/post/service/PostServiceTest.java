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
import kr.ac.knu.comit.fixture.PostFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.PostErrorCode;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostDailyVisitorRepository;
import kr.ac.knu.comit.post.domain.PostLikeRepository;
import kr.ac.knu.comit.post.domain.PostRepository;
import kr.ac.knu.comit.post.dto.HotPostListResponse;
import kr.ac.knu.comit.post.dto.PostDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


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
        // given
        // 조회 전/후 게시글 상태와 좋아요 여부를 준비한다.
        Post initialPost = PostFixture.post(10L, 7);
        Post reloadedPost = PostFixture.post(10L, 8);
        given(postRepository.findActiveById(10L))
                .willReturn(Optional.of(initialPost))
                .willReturn(Optional.of(reloadedPost));
        given(postLikeRepository.existsByPostIdAndMemberId(10L, 1L)).willReturn(true);

        // when
        // 게시글 상세 조회를 실행한다.
        PostDetailResponse response = postService.getPost(10L, 1L);

        // then
        // 최신 조회수와 후속 저장 호출 순서가 기대대로인지 확인한다.
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
        // given
        // 존재하지 않는 게시글 조회 상황을 준비한다.
        given(postRepository.findActiveById(10L)).willReturn(Optional.empty());

        // when & then
        // 상세 조회 시 POST_NOT_FOUND 예외가 발생해야 한다.
        assertThatThrownBy(() -> postService.getPost(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.POST_NOT_FOUND);

        // then
        // 조회수 증가나 방문자 기록 같은 후속 동작은 없어야 한다.
        then(postRepository).should().findActiveById(10L);
        then(postRepository).should(never()).incrementViewCount(anyLong());
        then(postRepository).shouldHaveNoMoreInteractions();
        then(postDailyVisitorRepository).shouldHaveNoInteractions();
        then(postLikeRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인기글 점수 순서대로 게시글을 재정렬하고 rank를 부여한다")
    void returnsHotPostsWithSequentialRanks() {
        // given
        // 인기글 집계 결과와 게시글 상세 조회 결과를 준비한다.
        Post olderPost = PostFixture.post(10L, 7);
        Post newerPost = PostFixture.post(20L, 3);

        given(postRepository.findHotPostScores(any(), any()))
                .willReturn(List.of(
                        hotPostScore(20L, 15),
                        hotPostScore(10L, 9)
                ));
        given(postRepository.findActiveWithMemberAndTagsByIds(List.of(20L, 10L)))
                .willReturn(List.of(olderPost, newerPost));
        given(commentQueryService.countActiveCommentsByPostIds(List.of(10L, 20L)))
                .willReturn(java.util.Map.of(10L, 2, 20L, 4));

        // when
        // 인기글 목록 조회를 실행한다.
        HotPostListResponse response = postService.getHotPosts();

        // then
        // 집계 순서대로 재정렬되고 rank가 순차적으로 부여되어야 한다.
        assertThat(response.posts()).hasSize(2);
        assertThat(response.posts().get(0).rank()).isEqualTo(1);
        assertThat(response.posts().get(0).id()).isEqualTo(20L);
        assertThat(response.posts().get(0).commentCount()).isEqualTo(4);
        assertThat(response.posts().get(1).rank()).isEqualTo(2);
        assertThat(response.posts().get(1).id()).isEqualTo(10L);
        assertThat(response.posts().get(1).commentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("인기글 후보가 없으면 빈 목록을 반환한다")
    void returnsEmptyHotPostsWhenNoCandidateExists() {
        // given
        // 인기글 후보가 하나도 없는 집계 결과를 준비한다.
        given(postRepository.findHotPostScores(any(), any())).willReturn(List.of());

        // when
        // 인기글 목록 조회를 실행한다.
        HotPostListResponse response = postService.getHotPosts();

        // then
        // 빈 목록을 반환하고 추가 상세 조회는 일어나지 않아야 한다.
        assertThat(response.posts()).isEmpty();
        then(postRepository).should(never()).findActiveWithMemberAndTagsByIds(any());
        then(commentQueryService).shouldHaveNoInteractions();
    }

    private PostRepository.HotPostScoreView hotPostScore(Long postId, long score) {
        return new PostRepository.HotPostScoreView() {
            @Override
            public Long getPostId() {
                return postId;
            }

            @Override
            public long getScore() {
                return score;
            }
        };
    }
}
