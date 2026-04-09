package kr.ac.knu.comit.comment.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.comment.domain.CommentLikeRepository;
import kr.ac.knu.comit.comment.domain.CommentRepository;
import kr.ac.knu.comit.comment.dto.CommentListResponse;
import kr.ac.knu.comit.comment.dto.CommentResponse;
import kr.ac.knu.comit.comment.dto.CreateCommentRequest;
import kr.ac.knu.comit.comment.dto.LikeToggleResponse;
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
    private final CommentLikeRepository commentLikeRepository;
    private final MemberService memberService;
    private final PostService postService;

    public CommentListResponse getComments(Long postId, Long memberId) {
        postService.getActivePostOrThrow(postId);
        List<Comment> topLevelComments = commentRepository.findActiveTopLevelByPostId(postId);
        List<Comment> replies = commentRepository.findActiveRepliesByPostId(postId);
        if (topLevelComments.isEmpty() && replies.isEmpty()) {
            return new CommentListResponse(List.of());
        }

        Set<Long> likedIds = Set.copyOf(commentLikeRepository.findLikedCommentIds(
                memberId,
                combineCommentIds(topLevelComments, replies)
        ));
        Map<Long, List<ReplyResponse>> repliesByParentId = groupRepliesByParentId(replies, likedIds, memberId);

        return new CommentListResponse(
                topLevelComments.stream()
                        .map(comment -> CommentResponse.from(
                                comment,
                                likedIds.contains(comment.getId()),
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
            commentRepository.softDeleteRepliesByParentCommentId(commentId, LocalDateTime.now());
        }
        comment.delete();
    }

    @Transactional
    public LikeToggleResponse toggleLike(Long commentId, Long memberId) {
        findCommentOrThrow(commentId);

        int inserted = commentLikeRepository.insertIgnore(commentId, memberId);
        if (inserted == 1) {
            commentRepository.incrementLikeCount(commentId);
            return LikeToggleResponse.likedState();
        }

        commentLikeRepository.deleteByCommentIdAndMemberId(commentId, memberId);
        commentRepository.decrementLikeCount(commentId);
        return LikeToggleResponse.unlikedState();
    }

    /**
     * 회원 삭제 시 댓글 좋아요 이력과 집계를 정리한다.
     */
    @Transactional
    public void removeMemberLikes(Long memberId) {
        List<Long> likedCommentIds = commentLikeRepository.findCommentIdsByMemberId(memberId);
        likedCommentIds.forEach(commentRepository::decrementLikeCount);
        commentLikeRepository.deleteAllByMemberId(memberId);
    }

    private Comment findCommentOrThrow(Long commentId) {
        return commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private List<Long> combineCommentIds(List<Comment> comments, List<Comment> replies) {
        return Stream.concat(comments.stream(), replies.stream())
                .map(Comment::getId)
                .toList();
    }

    private Map<Long, List<ReplyResponse>> groupRepliesByParentId(List<Comment> replies, Set<Long> likedIds, Long memberId) {
        return replies.stream()
                .collect(groupingBy(
                        Comment::getParentCommentId,
                        mapping(
                                reply -> ReplyResponse.from(
                                        reply,
                                        likedIds.contains(reply.getId()),
                                        reply.isWrittenBy(memberId)
                                ),
                                toList()
                        )
                ));
    }

    private void checkOwnership(Comment comment, Long memberId) {
        if (!comment.isWrittenBy(memberId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
