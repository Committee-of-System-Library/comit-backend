package kr.ac.knu.comit.comment.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.comment.domain.CommentHelpfulRepository;
import kr.ac.knu.comit.comment.domain.CommentRepository;
import kr.ac.knu.comit.comment.dto.CommentListResponse;
import kr.ac.knu.comit.comment.dto.CommentResponse;
import kr.ac.knu.comit.comment.dto.CreateCommentRequest;
import kr.ac.knu.comit.comment.dto.HelpfulToggleResponse;
import kr.ac.knu.comit.comment.dto.ReplyResponse;
import kr.ac.knu.comit.comment.dto.UpdateCommentRequest;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommentErrorCode;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentHelpfulRepository commentHelpfulRepository;
    private final MemberService memberService;
    private final PostService postService;

    public CommentListResponse getComments(Long postId, Long memberId) {
        postService.getActivePostOrThrow(postId);
        List<Comment> topLevelComments = commentRepository.findActiveTopLevelByPostId(postId);
        List<Comment> replies = commentRepository.findActiveRepliesByPostId(postId);
        if (topLevelComments.isEmpty() && replies.isEmpty()) {
            return new CommentListResponse(List.of());
        }

        Set<Long> helpfulIds = Set.copyOf(commentHelpfulRepository.findHelpfulCommentIds(
                memberId,
                combineCommentIds(topLevelComments, replies)
        ));
        Map<Long, List<ReplyResponse>> repliesByParentId = groupRepliesByParentId(replies, helpfulIds, memberId);

        return new CommentListResponse(
                topLevelComments.stream()
                        .map(comment -> CommentResponse.from(
                                comment,
                                helpfulIds.contains(comment.getId()),
                                comment.isWrittenBy(memberId),
                                repliesByParentId.getOrDefault(comment.getId(), List.of())
                        ))
                        .toList()
        );
    }

    @Transactional
    public Long createComment(Long postId, Long memberId, CreateCommentRequest request) {
        Post post = postService.getActivePostOrThrow(postId);
        Member author = memberService.findMemberOrThrow(memberId);
        Comment comment = request.parentCommentId() == null
                ? Comment.create(post, author, request.content())
                : Comment.reply(post, findCommentOrThrow(request.parentCommentId()), author, request.content());
        return commentRepository.save(comment).getId();
    }

    @Transactional
    public void updateComment(Long commentId, Long memberId, UpdateCommentRequest request) {
        Comment comment = findCommentOrThrow(commentId);
        checkOwnership(comment, memberId);
        comment.update(request.content());
    }

    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = findCommentOrThrow(commentId);
        checkOwnership(comment, memberId);
        if (!comment.isReply()) {
            commentRepository.findActiveRepliesByParentCommentId(commentId)
                    .forEach(Comment::delete);
        }
        comment.delete();
    }

    @Transactional
    public HelpfulToggleResponse toggleHelpful(Long commentId, Long memberId) {
        findCommentOrThrow(commentId);

        int inserted = commentHelpfulRepository.insertIgnore(commentId, memberId);
        if (inserted == 1) {
            commentRepository.incrementHelpfulCount(commentId);
            return HelpfulToggleResponse.helpfulState();
        }

        commentHelpfulRepository.deleteByCommentIdAndMemberId(commentId, memberId);
        commentRepository.decrementHelpfulCount(commentId);
        return HelpfulToggleResponse.notHelpfulState();
    }

    private Comment findCommentOrThrow(Long commentId) {
        return commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private List<Long> combineCommentIds(List<Comment> comments, List<Comment> replies) {
        return java.util.stream.Stream.concat(comments.stream(), replies.stream())
                .map(Comment::getId)
                .toList();
    }

    private Map<Long, List<ReplyResponse>> groupRepliesByParentId(List<Comment> replies, Set<Long> helpfulIds, Long memberId) {
        return replies.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Comment::getParentCommentId,
                        java.util.stream.Collectors.mapping(
                                reply -> ReplyResponse.from(
                                        reply,
                                        helpfulIds.contains(reply.getId()),
                                        reply.isWrittenBy(memberId)
                                ),
                                java.util.stream.Collectors.toList()
                        )
                ));
    }

    private void checkOwnership(Comment comment, Long memberId) {
        if (!comment.isWrittenBy(memberId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
