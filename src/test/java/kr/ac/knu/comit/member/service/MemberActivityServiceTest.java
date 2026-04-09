package kr.ac.knu.comit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.comment.domain.CommentRepository;
import kr.ac.knu.comit.fixture.CommentFixture;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.fixture.PostFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.dto.MyActivitySummaryResponse;
import kr.ac.knu.comit.member.dto.MyCommentCursorPageResponse;
import kr.ac.knu.comit.member.dto.MyLikedPostCursorPageResponse;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostLike;
import kr.ac.knu.comit.post.domain.PostLikeRepository;
import kr.ac.knu.comit.post.domain.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberActivityService")
class MemberActivityServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private MemberActivityService memberActivityService;

    @Nested
    @DisplayName("getActivitySummary")
    class GetActivitySummary {

        @Test
        @DisplayName("게시글/댓글/좋아요 수와 최근 3개 미리보기를 반환한다")
        void returnsCountsAndRecentItems() {
            // given
            // 카운트 및 최근 항목 데이터를 준비한다.
            given(postRepository.countMyPosts(1L)).willReturn(15L);
            given(commentRepository.countMyComments(1L)).willReturn(42L);
            given(postLikeRepository.countMyLikes(1L)).willReturn(8L);

            Post post = PostFixture.post(10L);
            given(postRepository.findMyPosts(1L, null, 3)).willReturn(List.of(post));

            Comment comment = CommentFixture.topLevelComment(20L, post, MemberFixture.member(1L), "좋은 글이에요", 0);
            given(commentRepository.findMyComments(1L, null, 3)).willReturn(List.of(comment));

            PostLike like = postLike(1L, 10L, 1L);
            given(postLikeRepository.findMyLikes(1L, null, 3)).willReturn(List.of(like));
            given(postRepository.findActiveWithMemberAndTagsByIds(List.of(10L))).willReturn(List.of(post));

            // when
            // 내 활동 요약 조회를 실행한다.
            MyActivitySummaryResponse response = memberActivityService.getActivitySummary(1L);

            // then
            // 카운트와 최근 항목이 모두 올바르게 반환되어야 한다.
            assertThat(response.postCount()).isEqualTo(15L);
            assertThat(response.commentCount()).isEqualTo(42L);
            assertThat(response.likeCount()).isEqualTo(8L);
            assertThat(response.recentPosts()).hasSize(1);
            assertThat(response.recentPosts().getFirst().id()).isEqualTo(10L);
            assertThat(response.recentComments()).hasSize(1);
            assertThat(response.recentComments().getFirst().content()).isEqualTo("좋은 글이에요");
            assertThat(response.recentLikes()).hasSize(1);
            assertThat(response.recentLikes().getFirst().postId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("좋아요한 게시글이 삭제/숨김 처리된 경우 최근 좋아요 목록에서 제외한다")
        void excludesDeletedOrHiddenPostsFromRecentLikes() {
            // given
            // 삭제된 게시글에 좋아요가 남아 있는 상황을 준비한다.
            given(postRepository.countMyPosts(1L)).willReturn(0L);
            given(commentRepository.countMyComments(1L)).willReturn(0L);
            given(postLikeRepository.countMyLikes(1L)).willReturn(0L);
            given(postRepository.findMyPosts(1L, null, 3)).willReturn(List.of());
            given(commentRepository.findMyComments(1L, null, 3)).willReturn(List.of());

            PostLike likeOnDeletedPost = postLike(1L, 99L, 1L);
            given(postLikeRepository.findMyLikes(1L, null, 3)).willReturn(List.of(likeOnDeletedPost));
            given(postRepository.findActiveWithMemberAndTagsByIds(List.of(99L))).willReturn(List.of());

            // when
            // 내 활동 요약 조회를 실행한다.
            MyActivitySummaryResponse response = memberActivityService.getActivitySummary(1L);

            // then
            // 삭제된 게시글 좋아요는 목록에 포함되지 않아야 한다.
            assertThat(response.recentLikes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMyComments")
    class GetMyComments {

        @Test
        @DisplayName("size가 0 이하이면 INVALID_REQUEST 예외를 던진다")
        void throwsWhenSizeIsZeroOrNegative() {
            // when & then
            // size=0 요청은 유효하지 않은 요청으로 거부된다.
            assertThatThrownBy(() -> memberActivityService.getMyComments(1L, null, 0))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("cursor가 없으면 첫 페이지를 반환하고 hasNext는 false다")
        void returnsFirstPageWithoutCursor() {
            // given
            // 댓글이 1개인 첫 페이지 상황을 준비한다.
            Post post = PostFixture.post(10L);
            Comment comment = CommentFixture.topLevelComment(20L, post, MemberFixture.member(1L), "댓글 내용", 0);
            given(commentRepository.findMyComments(1L, null, 21)).willReturn(List.of(comment));

            // when
            // 첫 페이지 댓글 목록을 조회한다.
            MyCommentCursorPageResponse response = memberActivityService.getMyComments(1L, null, 20);

            // then
            // 댓글 1개가 반환되고 다음 페이지가 없어야 한다.
            assertThat(response.comments()).hasSize(1);
            assertThat(response.comments().getFirst().content()).isEqualTo("댓글 내용");
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursorId()).isNull();
        }

        @Test
        @DisplayName("결과가 size+1개이면 hasNext를 true로 반환하고 마지막 표시 항목 ID를 nextCursorId로 설정한다")
        void setsNextCursorIdToLastVisibleItemWhenHasNext() {
            // given
            // size=2 요청에 3개 결과(hasNext 판별용 1개 포함)를 준비한다.
            Post post = PostFixture.post(10L);
            Comment c1 = CommentFixture.topLevelComment(30L, post, MemberFixture.member(1L), "댓글1", 0);
            Comment c2 = CommentFixture.topLevelComment(20L, post, MemberFixture.member(1L), "댓글2", 0);
            Comment c3 = CommentFixture.topLevelComment(10L, post, MemberFixture.member(1L), "댓글3", 0);
            given(commentRepository.findMyComments(1L, null, 3)).willReturn(List.of(c1, c2, c3));

            // when
            // size=2로 댓글 목록을 조회한다.
            MyCommentCursorPageResponse response = memberActivityService.getMyComments(1L, null, 2);

            // then
            // 2개만 반환되고 nextCursorId는 마지막 표시 댓글(c2) ID여야 한다.
            assertThat(response.comments()).hasSize(2);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursorId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("size가 20을 초과하면 20으로 제한하여 조회한다")
        void capsPageSizeAt20() {
            // given
            // 빈 결과를 반환하도록 준비한다.
            given(commentRepository.findMyComments(1L, null, 21)).willReturn(List.of());

            // when
            // size=100으로 댓글 목록을 조회한다.
            memberActivityService.getMyComments(1L, null, 100);

            // then
            // 리포지토리 호출은 21(cap 20 + hasNext 판별 1)로 제한되어야 한다.
            org.mockito.BDDMockito.then(commentRepository).should()
                    .findMyComments(1L, null, 21);
        }
    }

    @Nested
    @DisplayName("getMyLikes")
    class GetMyLikes {

        @Test
        @DisplayName("size가 0 이하이면 INVALID_REQUEST 예외를 던진다")
        void throwsWhenSizeIsZeroOrNegative() {
            // when & then
            // size=0 요청은 유효하지 않은 요청으로 거부된다.
            assertThatThrownBy(() -> memberActivityService.getMyLikes(1L, null, 0))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("좋아요한 게시글이 삭제/숨김 처리된 경우 목록에서 제외한다")
        void excludesDeletedOrHiddenPostsFromLikeList() {
            // given
            // 활성 게시글 좋아요와 삭제된 게시글 좋아요가 함께 존재하는 상황을 준비한다.
            PostLike likeOnActive = postLike(2L, 10L, 1L);
            PostLike likeOnDeleted = postLike(1L, 99L, 1L);
            given(postLikeRepository.findMyLikes(1L, null, 21)).willReturn(List.of(likeOnActive, likeOnDeleted));

            Post activePost = PostFixture.post(10L);
            given(postRepository.findActiveWithMemberAndTagsByIds(List.of(10L, 99L)))
                    .willReturn(List.of(activePost));

            // when
            // 내가 좋아요한 게시글 목록을 조회한다.
            MyLikedPostCursorPageResponse response = memberActivityService.getMyLikes(1L, null, 20);

            // then
            // 활성 게시글만 반환되어야 한다.
            assertThat(response.posts()).hasSize(1);
            assertThat(response.posts().getFirst().postId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("결과가 size+1개이면 hasNext를 true로 반환하고 nextCursorId를 설정한다")
        void setsNextCursorIdWhenHasNext() {
            // given
            // size=1 요청에 2개 결과(hasNext 판별용 1개 포함)를 준비한다.
            PostLike like1 = postLike(20L, 10L, 1L);
            PostLike like2 = postLike(10L, 20L, 1L);
            given(postLikeRepository.findMyLikes(1L, null, 2)).willReturn(List.of(like1, like2));

            Post post1 = PostFixture.post(10L);
            Post post2 = PostFixture.post(20L);
            given(postRepository.findActiveWithMemberAndTagsByIds(List.of(10L, 20L)))
                    .willReturn(List.of(post1, post2));

            // when
            // size=1로 좋아요 목록을 조회한다.
            MyLikedPostCursorPageResponse response = memberActivityService.getMyLikes(1L, null, 1);

            // then
            // 1개만 반환되고 nextCursorId는 마지막 표시 PostLike(like1) ID여야 한다.
            assertThat(response.posts()).hasSize(1);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursorId()).isEqualTo(20L);
        }
    }

    private PostLike postLike(Long id, Long postId, Long memberId) {
        PostLike like = PostLike.of(postId, memberId);
        ReflectionTestUtils.setField(like, "id", id);
        return like;
    }
}
