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
import kr.ac.knu.comit.post.domain.PostImageRepository;
import kr.ac.knu.comit.post.domain.PostLikeRepository;
import kr.ac.knu.comit.post.domain.PostRepository;
import kr.ac.knu.comit.post.dto.HotPostListResponse;
import kr.ac.knu.comit.post.dto.PostDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.dto.CreatePostRequest;
import kr.ac.knu.comit.post.dto.LikeToggleResponse;
import kr.ac.knu.comit.post.dto.PostCursorPageResponse;
import kr.ac.knu.comit.post.dto.UpdatePostRequest;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;

import kr.ac.knu.comit.post.config.HotPostPolicyProperties;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostDailyVisitorRepository postDailyVisitorRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private CommentQueryService commentQueryService;

    @Mock
    private ContentPreviewGenerator contentPreviewGenerator;

    @Mock
    private HotPostPolicyProperties hotPostPolicy;

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

        given(hotPostPolicy.getWindowDays()).willReturn(7);
        given(hotPostPolicy.getExcludedBoardTypes()).willReturn(List.of());
        given(hotPostPolicy.getLikeWeight()).willReturn(5);
        given(hotPostPolicy.getCommentWeight()).willReturn(3);
        given(hotPostPolicy.getVisitorWeight()).willReturn(2);
        given(hotPostPolicy.getLimit()).willReturn(5);
        given(postRepository.findHotPostScores(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), any(), anyInt()))
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
        given(hotPostPolicy.getWindowDays()).willReturn(7);
        given(hotPostPolicy.getExcludedBoardTypes()).willReturn(List.of());
        given(hotPostPolicy.getLikeWeight()).willReturn(5);
        given(hotPostPolicy.getCommentWeight()).willReturn(3);
        given(hotPostPolicy.getVisitorWeight()).willReturn(2);
        given(hotPostPolicy.getLimit()).willReturn(5);
        given(postRepository.findHotPostScores(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), any(), anyInt())).willReturn(List.of());

        // when
        // 인기글 목록 조회를 실행한다.
        HotPostListResponse response = postService.getHotPosts();

        // then
        // 빈 목록을 반환하고 추가 상세 조회는 일어나지 않아야 한다.
        assertThat(response.posts()).isEmpty();
        then(postRepository).should(never()).findActiveWithMemberAndTagsByIds(any());
        then(commentQueryService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회원 삭제 정리 시 게시글 좋아요 수와 방문 기록을 함께 정리한다")
    void removesMemberInteractions() {
        // given
        // 삭제 대상 회원이 남긴 게시글 좋아요와 방문 기록을 준비한다.
        given(postLikeRepository.findPostIdsByMemberId(1L)).willReturn(List.of(10L, 20L));

        // when
        // 회원 연관 게시글 활동 정리를 실행한다.
        postService.removeMemberInteractions(1L);

        // then
        // 좋아요 집계를 먼저 보정하고 row 및 방문 기록을 제거해야 한다.
        then(postLikeRepository).should().findPostIdsByMemberId(1L);
        then(postRepository).should().decrementLikeCount(10L);
        then(postRepository).should().decrementLikeCount(20L);
        then(postLikeRepository).should().deleteAllByMemberId(1L);
        then(postDailyVisitorRepository).should().deleteAllByMemberId(1L);
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("회원 조회 후 게시글을 저장하고 ID를 반환한다")
        void returnsPostIdAfterSaving() {
            // given
            // 게시글 작성자와 저장 후 ID를 준비한다.
            given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L));
            given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
                Post saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 100L);
                return saved;
            });

            // when
            // 게시글 생성을 실행한다.
            Long postId = postService.createPost(1L, new CreatePostRequest(BoardType.QNA, "제목", "본문", List.of(), List.of()));

            // then
            // 저장된 게시글 ID가 반환되어야 한다.
            assertThat(postId).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("작성자이면 제목과 본문을 수정한다")
        void updatesPostWhenCallerIsOwner() {
            // given
            // 작성자(99L)가 자신의 게시글을 수정하는 상황을 준비한다.
            Post post = PostFixture.post(10L);
            given(postRepository.findActiveById(10L)).willReturn(Optional.of(post));

            // when
            // 게시글 수정을 실행한다.
            postService.updatePost(99L, 10L, new UpdatePostRequest("새 제목", "새 본문", List.of(), List.of()));

            // then
            // 제목과 본문이 새 값으로 바뀌어야 한다.
            assertThat(post.getTitle()).isEqualTo("새 제목");
            assertThat(post.getContent()).isEqualTo("새 본문");
        }

        @Test
        @DisplayName("작성자가 아니면 FORBIDDEN 예외를 던진다")
        void throwsWhenCallerIsNotOwner() {
            // given
            // 타인(1L)이 작성자(99L)의 게시글을 수정하려는 상황을 준비한다.
            Post post = PostFixture.post(10L);
            given(postRepository.findActiveById(10L)).willReturn(Optional.of(post));

            // when & then
            // 소유권 검사에서 FORBIDDEN 예외가 발생해야 한다.
            assertThatThrownBy(() -> postService.updatePost(1L, 10L, new UpdatePostRequest("새 제목", "새 본문", List.of(), List.of())))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("작성자이면 게시글을 삭제 상태로 변경한다")
        void deletesPostWhenCallerIsOwner() {
            // given
            // 작성자(99L)가 자신의 게시글을 삭제하는 상황을 준비한다.
            Post post = PostFixture.post(10L);
            given(postRepository.findActiveById(10L)).willReturn(Optional.of(post));

            // when
            // 게시글 삭제를 실행한다.
            postService.deletePost(99L, 10L);

            // then
            // 게시글이 삭제 상태여야 한다.
            assertThat(post.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("작성자가 아니면 FORBIDDEN 예외를 던지고 게시글은 삭제되지 않는다")
        void throwsAndKeepsPostActiveWhenCallerIsNotOwner() {
            // given
            // 타인(1L)이 작성자(99L)의 게시글을 삭제하려는 상황을 준비한다.
            Post post = PostFixture.post(10L);
            given(postRepository.findActiveById(10L)).willReturn(Optional.of(post));

            // when & then
            // 소유권 검사에서 FORBIDDEN 예외가 발생해야 한다.
            assertThatThrownBy(() -> postService.deletePost(1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.FORBIDDEN);
            assertThat(post.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("관리자는 작성자와 무관하게 게시글을 삭제 상태로 변경한다")
        void deletesPostWhenCallerIsAdmin() {
            // given
            // 관리자가 타인(99L)의 게시글을 삭제하는 상황을 준비한다.
            Post post = PostFixture.post(10L);
            given(postRepository.findActiveById(10L)).willReturn(Optional.of(post));

            // when
            // 관리자 경로로 게시글 삭제를 실행한다.
            postService.deletePost(1L, 10L, true);

            // then
            // 소유권 검증 없이 게시글이 삭제 상태여야 한다.
            assertThat(post.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("toggleLike")
    class ToggleLike {

        @Test
        @DisplayName("처음 좋아요를 누르면 liked 상태를 반환하고 likeCount를 올린다")
        void returnsLikedStateAndIncrementsCountOnFirstLike() {
            // given
            // 좋아요 이력이 없는 게시글 상태를 준비한다.
            given(postRepository.findActiveById(10L)).willReturn(Optional.of(PostFixture.post(10L)));
            given(postLikeRepository.insertIgnore(10L, 1L)).willReturn(1);

            // when
            // 좋아요 토글을 실행한다.
            LikeToggleResponse response = postService.toggleLike(1L, 10L);

            // then
            // liked 상태와 카운트 증가 쿼리가 함께 반영되어야 한다.
            assertThat(response.liked()).isTrue();
            then(postRepository).should().incrementLikeCount(10L);
        }

        @Test
        @DisplayName("이미 좋아요 상태에서 다시 누르면 unliked 상태를 반환하고 likeCount를 내린다")
        void returnsUnlikedStateAndDecrementsCountOnSecondLike() {
            // given
            // 이미 좋아요를 누른 상태를 준비한다.
            given(postRepository.findActiveById(10L)).willReturn(Optional.of(PostFixture.post(10L)));
            given(postLikeRepository.insertIgnore(10L, 1L)).willReturn(0);

            // when
            // 좋아요 토글을 다시 실행한다.
            LikeToggleResponse response = postService.toggleLike(1L, 10L);

            // then
            // 좋아요 취소 처리와 카운트 감소 쿼리가 호출되어야 한다.
            assertThat(response.liked()).isFalse();
            then(postLikeRepository).should().deleteByPostIdAndMemberId(10L, 1L);
            then(postRepository).should().decrementLikeCount(10L);
        }

        @Test
        @DisplayName("존재하지 않는 게시글에 좋아요를 누르면 POST_NOT_FOUND 예외를 던진다")
        void throwsWhenPostNotFound() {
            // given
            // 존재하지 않는 게시글 조회 상황을 준비한다.
            given(postRepository.findActiveById(10L)).willReturn(Optional.empty());

            // when & then
            // POST_NOT_FOUND 예외가 발생해야 한다.
            assertThatThrownBy(() -> postService.toggleLike(1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(PostErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getPosts")
    class GetPosts {

        @Test
        @DisplayName("size가 0 이하이면 INVALID_REQUEST 예외를 던진다")
        void throwsWhenSizeIsZeroOrNegative() {
            // when & then
            // size=0 요청은 유효하지 않은 요청으로 거부된다.
            assertThatThrownBy(() -> postService.getPosts(BoardType.QNA, null, 0))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("size가 20을 초과하면 20으로 제한하여 조회한다")
        void capsPageSizeAt20() {
            // given
            // cursor가 null인 첫 페이지 조회 상황을 준비한다.
            given(postRepository.findFirstPage(eq(BoardType.QNA), any(Pageable.class))).willReturn(List.of());
            given(commentQueryService.countActiveCommentsByPostIds(anyList())).willReturn(Map.of());

            // when
            // size=100으로 첫 페이지 조회를 실행한다.
            postService.getPosts(BoardType.QNA, null, 100);

            // then
            // 실제로 리포지토리에 전달된 pageSize는 21(cap 20 + hasNext 판별 1)이어야 한다.
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            then(postRepository).should().findFirstPage(eq(BoardType.QNA), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(21);
        }
    }

    private PostRepository.HotPostScoreView hotPostScore(Long postId, double score) {
        return new PostRepository.HotPostScoreView() {
            @Override
            public Long getPostId() {
                return postId;
            }

            @Override
            public double getScore() {
                return score;
            }
        };
    }
}
