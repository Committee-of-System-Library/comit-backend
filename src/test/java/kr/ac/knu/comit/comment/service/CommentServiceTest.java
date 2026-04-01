package kr.ac.knu.comit.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.comment.domain.CommentLikeRepository;
import kr.ac.knu.comit.comment.domain.CommentRepository;
import kr.ac.knu.comit.comment.dto.CommentListResponse;
import kr.ac.knu.comit.comment.dto.CreateCommentRequest;
import kr.ac.knu.comit.comment.dto.LikeToggleResponse;
import kr.ac.knu.comit.fixture.CommentFixture;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.fixture.PostFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommentErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import kr.ac.knu.comit.comment.dto.UpdateCommentRequest;
import kr.ac.knu.comit.global.exception.CommonErrorCode;


@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private PostService postService;

    @InjectMocks
    private CommentService commentService;

    @Nested
    @DisplayName("getComments")
    class GetComments {

        @Test
        @DisplayName("대댓글을 부모 댓글 아래 replies로 묶어 반환한다")
        void groupsRepliesUnderParents() {
            // given
            // 부모 댓글과 대댓글이 함께 존재하는 댓글 목록 상황을 준비한다.
            Post post = PostFixture.post(10L);
            Comment parent = CommentFixture.topLevelComment(201L, post, MemberFixture.member(2L, "parent"), "부모 댓글", 4);
            Comment reply = CommentFixture.replyComment(202L, post, parent, MemberFixture.member(1L, "me"), "대댓글", 1);

            given(postService.getActivePostOrThrow(10L)).willReturn(post);
            given(commentRepository.findActiveTopLevelByPostId(10L)).willReturn(List.of(parent));
            given(commentRepository.findActiveRepliesByPostId(10L)).willReturn(List.of(reply));
            given(commentLikeRepository.findLikedCommentIds(1L, List.of(201L, 202L)))
                    .willReturn(List.of(202L));

            // when
            // 댓글 목록 조회를 실행한다.
            CommentListResponse response = commentService.getComments(10L, 1L);

            // then
            // 대댓글이 부모 아래로 묶이고 사용자 상태가 함께 계산되는지 확인한다.
            assertThat(response.comments()).hasSize(1);
            assertThat(response.comments().getFirst().id()).isEqualTo(201L);
            assertThat(response.comments().getFirst().likeCount()).isEqualTo(4);
            assertThat(response.comments().getFirst().likedByMe()).isFalse();
            assertThat(response.comments().getFirst().replies()).hasSize(1);
            assertThat(response.comments().getFirst().replies().getFirst().id()).isEqualTo(202L);
            assertThat(response.comments().getFirst().replies().getFirst().likeCount()).isEqualTo(1);
            assertThat(response.comments().getFirst().replies().getFirst().likedByMe()).isTrue();
            assertThat(response.comments().getFirst().replies().getFirst().mine()).isTrue();
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("parentCommentId가 있으면 대댓글을 생성한다")
        void createsReplyWhenParentCommentIdIsPresent() {
            // given
            // 대댓글 생성에 필요한 게시글, 작성자, 부모 댓글을 준비한다.
            Post post = PostFixture.post(10L);
            Member author = MemberFixture.member(1L, "writer");
            Comment parent = CommentFixture.topLevelComment(201L, post, MemberFixture.member(2L, "parent"), "부모 댓글", 0);
            CreateCommentRequest request = new CreateCommentRequest(201L, "대댓글");

            given(postService.getActivePostOrThrow(10L)).willReturn(post);
            given(memberService.findMemberOrThrow(1L)).willReturn(author);
            given(commentRepository.findActiveById(201L)).willReturn(Optional.of(parent));
            given(commentRepository.save(org.mockito.ArgumentMatchers.any(Comment.class)))
                    .willAnswer(invocation -> {
                        Comment saved = invocation.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", 202L);
                        return saved;
                    });

            // when
            // parentCommentId를 포함한 댓글 생성을 실행한다.
            Long commentId = commentService.createComment(10L, 1L, request);

            // then
            // 새로 생성된 대댓글 ID가 반환되어야 한다.
            assertThat(commentId).isEqualTo(202L);
        }

        @Test
        @DisplayName("대댓글의 대댓글을 생성하려 하면 INVALID_PARENT_COMMENT 예외를 던진다")
        void throwsWhenParentCommentIsAlreadyReply() {
            // given
            // 이미 대댓글인 댓글을 부모로 지정한 요청을 준비한다.
            Post post = PostFixture.post(10L);
            Member author = MemberFixture.member(1L, "writer");
            Comment parent = CommentFixture.topLevelComment(201L, post, MemberFixture.member(2L, "parent"), "부모 댓글", 0);
            Comment reply = CommentFixture.replyComment(202L, post, parent, MemberFixture.member(3L, "child"), "대댓글", 0);
            CreateCommentRequest request = new CreateCommentRequest(202L, "대대댓글");

            given(postService.getActivePostOrThrow(10L)).willReturn(post);
            given(memberService.findMemberOrThrow(1L)).willReturn(author);
            given(commentRepository.findActiveById(202L)).willReturn(Optional.of(reply));

            // when & then
            // 대댓글의 대댓글 생성 시도가 도메인 규칙으로 차단되는지 확인한다.
            assertThatThrownBy(() -> commentService.createComment(10L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommentErrorCode.INVALID_PARENT_COMMENT);
        }
    }

    @Nested
    @DisplayName("toggleLike")
    class ToggleLike {

        @Test
        @DisplayName("처음 좋아요를 누르면 카운트를 올리고 likedState를 반환한다")
        void incrementsCountAndReturnsLikedStateOnFirstToggle() {
            // given
            // 아직 좋아요 이력이 없는 댓글 상태를 준비한다.
            Post post = PostFixture.post(10L);
            Comment comment = CommentFixture.topLevelComment(201L, post, MemberFixture.member(2L, "writer"), "댓글", 0);

            given(commentRepository.findActiveById(201L)).willReturn(Optional.of(comment));
            given(commentLikeRepository.insertIgnore(201L, 1L)).willReturn(1);

            // when
            // 좋아요 토글을 한 번 실행한다.
            LikeToggleResponse response = commentService.toggleLike(201L, 1L);

            // then
            // 좋아요 상태와 카운트 증가 쿼리가 함께 반영되어야 한다.
            assertThat(response.liked()).isTrue();
            then(commentRepository).should().incrementLikeCount(201L);
        }

        @Test
        @DisplayName("이미 좋아요 상태에서 다시 누르면 카운트를 내리고 unlikedState를 반환한다")
        void decrementsCountAndReturnsUnlikedStateOnSecondToggle() {
            // given
            // 이미 좋아요 상태인 댓글을 준비한다.
            Post post = PostFixture.post(10L);
            Comment comment = CommentFixture.topLevelComment(201L, post, MemberFixture.member(2L, "writer"), "댓글", 1);

            given(commentRepository.findActiveById(201L)).willReturn(Optional.of(comment));
            given(commentLikeRepository.insertIgnore(201L, 1L)).willReturn(0);

            // when
            // 좋아요 토글을 다시 실행한다.
            LikeToggleResponse response = commentService.toggleLike(201L, 1L);

            // then
            // 좋아요 취소 처리와 카운트 감소 쿼리가 호출되어야 한다.
            assertThat(response.liked()).isFalse();
            then(commentLikeRepository).should().deleteByCommentIdAndMemberId(201L, 1L);
            then(commentRepository).should().decrementLikeCount(201L);
        }

        @Test
        @DisplayName("존재하지 않는 댓글에 좋아요를 누르면 COMMENT_NOT_FOUND 예외를 던진다")
        void throwsWhenCommentNotFound() {
            // given
            // 존재하지 않는 댓글 ID 조회 결과를 준비한다.
            given(commentRepository.findActiveById(999L)).willReturn(Optional.empty());

            // when & then
            // 좋아요 토글 시도가 COMMENT_NOT_FOUND로 변환되는지 확인한다.
            assertThatThrownBy(() -> commentService.toggleLike(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        @Test
        @DisplayName("작성자이면 댓글 내용을 수정한다")
        void updatesCommentWhenCallerIsOwner() {
            // given
            // 작성자(1L)가 자신의 댓글을 수정하는 상황을 준비한다.
            Post post = PostFixture.post(10L);
            Comment comment = CommentFixture.topLevelComment(201L, post, MemberFixture.member(1L, "writer"), "기존 내용", 0);
            given(commentRepository.findActiveById(201L)).willReturn(Optional.of(comment));

            // when
            // 댓글 수정을 실행한다.
            commentService.updateComment(201L, 1L, new UpdateCommentRequest("수정된 내용"));

            // then
            // 댓글 내용이 새 값으로 바뀌어야 한다.
            assertThat(comment.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("작성자가 아니면 FORBIDDEN 예외를 던진다")
        void throwsWhenCallerIsNotOwner() {
            // given
            // 타인(2L)이 작성자(1L)의 댓글을 수정하려는 상황을 준비한다.
            Post post = PostFixture.post(10L);
            Comment comment = CommentFixture.topLevelComment(201L, post, MemberFixture.member(1L, "writer"), "기존 내용", 0);
            given(commentRepository.findActiveById(201L)).willReturn(Optional.of(comment));

            // when & then
            // 소유권 검사에서 FORBIDDEN 예외가 발생해야 한다.
            assertThatThrownBy(() -> commentService.updateComment(201L, 2L, new UpdateCommentRequest("수정된 내용")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("최상위 댓글을 삭제하면 대댓글도 벌크 쿼리로 함께 삭제한다")
        void deletesRepliesTogetherWhenDeletingTopLevelComment() {
            // given
            // 대댓글을 가질 수 있는 최상위 댓글을 준비한다.
            Post post = PostFixture.post(10L);
            Member author = MemberFixture.member(1L, "writer");
            Comment parent = CommentFixture.topLevelComment(201L, post, author, "부모 댓글", 0);

            given(commentRepository.findActiveById(201L)).willReturn(Optional.of(parent));

            // when
            // 최상위 댓글 삭제를 실행한다.
            commentService.deleteComment(201L, 1L);

            // then
            // 부모 댓글 soft delete와 대댓글 벌크 삭제가 함께 수행되어야 한다.
            then(commentRepository).should().softDeleteRepliesByParentCommentId(
                    org.mockito.ArgumentMatchers.eq(201L),
                    org.mockito.ArgumentMatchers.any(LocalDateTime.class)
            );
            assertThat(ReflectionTestUtils.getField(parent, "deletedAt")).isInstanceOf(LocalDateTime.class);
        }

        @Test
        @DisplayName("대댓글을 삭제하면 softDeleteReplies를 호출하지 않는다")
        void doesNotDeleteRepliesWhenDeletingReplyComment() {
            // given
            // 대댓글(reply)인 댓글을 준비한다.
            Post post = PostFixture.post(10L);
            Comment parent = CommentFixture.topLevelComment(201L, post, MemberFixture.member(2L, "parent"), "부모 댓글", 0);
            Comment reply = CommentFixture.replyComment(202L, post, parent, MemberFixture.member(1L, "writer"), "대댓글", 0);
            given(commentRepository.findActiveById(202L)).willReturn(Optional.of(reply));

            // when
            // 대댓글 삭제를 실행한다.
            commentService.deleteComment(202L, 1L);

            // then
            // 대댓글은 자신만 삭제되고 softDeleteReplies는 호출되지 않아야 한다.
            then(commentRepository).should(never()).softDeleteRepliesByParentCommentId(
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.any(LocalDateTime.class)
            );
            assertThat(ReflectionTestUtils.getField(reply, "deletedAt")).isInstanceOf(LocalDateTime.class);
        }

        @Test
        @DisplayName("작성자가 아니면 FORBIDDEN 예외를 던진다")
        void throwsWhenCallerIsNotOwner() {
            // given
            // 타인(2L)이 작성자(1L)의 댓글을 삭제하려는 상황을 준비한다.
            Post post = PostFixture.post(10L);
            Comment comment = CommentFixture.topLevelComment(201L, post, MemberFixture.member(1L, "writer"), "댓글", 0);
            given(commentRepository.findActiveById(201L)).willReturn(Optional.of(comment));

            // when & then
            // 소유권 검사에서 FORBIDDEN 예외가 발생해야 한다.
            assertThatThrownBy(() -> commentService.deleteComment(201L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.FORBIDDEN);
        }
    }

}
