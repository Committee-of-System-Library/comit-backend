package kr.ac.knu.comit.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.comment.domain.CommentHelpfulRepository;
import kr.ac.knu.comit.comment.domain.CommentRepository;
import kr.ac.knu.comit.comment.dto.CommentListResponse;
import kr.ac.knu.comit.comment.dto.CreateCommentRequest;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommentErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.domain.BoardType;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentHelpfulRepository commentHelpfulRepository;

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
            Post post = post(10L);
            Comment parent = topLevelComment(201L, post, member(2L, "parent"), "부모 댓글", 4);
            Comment reply = replyComment(202L, post, parent, member(1L, "me"), "대댓글", 1);

            given(postService.getActivePostOrThrow(10L)).willReturn(post);
            given(commentRepository.findActiveTopLevelByPostId(10L)).willReturn(List.of(parent));
            given(commentRepository.findActiveRepliesByPostId(10L)).willReturn(List.of(reply));
            given(commentHelpfulRepository.findHelpfulCommentIds(1L, List.of(201L, 202L)))
                    .willReturn(List.of(202L));

            CommentListResponse response = commentService.getComments(10L, 1L);

            assertThat(response.comments()).hasSize(1);
            assertThat(response.comments().getFirst().id()).isEqualTo(201L);
            assertThat(response.comments().getFirst().replies()).hasSize(1);
            assertThat(response.comments().getFirst().replies().getFirst().id()).isEqualTo(202L);
            assertThat(response.comments().getFirst().replies().getFirst().helpfulByMe()).isTrue();
            assertThat(response.comments().getFirst().replies().getFirst().mine()).isTrue();
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("parentCommentId가 있으면 대댓글을 생성한다")
        void createsReplyWhenParentCommentIdIsPresent() {
            Post post = post(10L);
            Member author = member(1L, "writer");
            Comment parent = topLevelComment(201L, post, member(2L, "parent"), "부모 댓글", 0);
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

            Long commentId = commentService.createComment(10L, 1L, request);

            assertThat(commentId).isEqualTo(202L);
        }

        @Test
        @DisplayName("대댓글의 대댓글을 생성하려 하면 INVALID_PARENT_COMMENT 예외를 던진다")
        void throwsWhenParentCommentIsAlreadyReply() {
            Post post = post(10L);
            Member author = member(1L, "writer");
            Comment parent = topLevelComment(201L, post, member(2L, "parent"), "부모 댓글", 0);
            Comment reply = replyComment(202L, post, parent, member(3L, "child"), "대댓글", 0);
            CreateCommentRequest request = new CreateCommentRequest(202L, "대대댓글");

            given(postService.getActivePostOrThrow(10L)).willReturn(post);
            given(memberService.findMemberOrThrow(1L)).willReturn(author);
            given(commentRepository.findActiveById(202L)).willReturn(Optional.of(reply));

            assertThatThrownBy(() -> commentService.createComment(10L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommentErrorCode.INVALID_PARENT_COMMENT);
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("최상위 댓글을 삭제하면 대댓글도 함께 삭제한다")
        void deletesRepliesTogetherWhenDeletingTopLevelComment() {
            Post post = post(10L);
            Member author = member(1L, "writer");
            Comment parent = topLevelComment(201L, post, author, "부모 댓글", 0);
            Comment reply = replyComment(202L, post, parent, member(2L, "child"), "대댓글", 0);

            given(commentRepository.findActiveById(201L)).willReturn(Optional.of(parent));
            given(commentRepository.findActiveRepliesByParentCommentId(201L)).willReturn(List.of(reply));

            commentService.deleteComment(201L, 1L);

            then(commentRepository).should().findActiveRepliesByParentCommentId(201L);
            assertThat(ReflectionTestUtils.getField(parent, "deletedAt")).isInstanceOf(LocalDateTime.class);
            assertThat(ReflectionTestUtils.getField(reply, "deletedAt")).isInstanceOf(LocalDateTime.class);
        }
    }

    private Post post(Long id) {
        Member author = member(99L, "post-writer");
        Post post = Post.create(author, BoardType.QNA, "질문", "본문", List.of());
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Member member(Long id, String nickname) {
        Member member = Member.create("sso-" + id, nickname, "20230001");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Comment topLevelComment(Long id, Post post, Member author, String content, int helpfulCount) {
        Comment comment = Comment.create(post, author, content);
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "helpfulCount", helpfulCount);
        return comment;
    }

    private Comment replyComment(Long id, Post post, Comment parent, Member author, String content, int helpfulCount) {
        Comment comment = Comment.reply(post, parent, author, content);
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "helpfulCount", helpfulCount);
        return comment;
    }
}
