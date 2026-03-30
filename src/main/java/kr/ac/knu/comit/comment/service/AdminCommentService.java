package kr.ac.knu.comit.comment.service;

import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.comment.domain.CommentRepository;
import kr.ac.knu.comit.comment.dto.AdminCommentPageResponse;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommentErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminCommentService {

    private final CommentRepository commentRepository;

    public AdminCommentPageResponse getComments(Long postId, Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findAllActiveForAdmin(postId, pageable);
        return AdminCommentPageResponse.from(commentPage);
    }

    @Transactional
    public void hideComment(Long commentId) {
        Comment comment = findCommentOrThrow(commentId);
        comment.hideByAdmin();
    }

    @Transactional
    public void restoreComment(Long commentId) {
        Comment comment = findCommentOrThrow(commentId);
        comment.restoreByAdmin();
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = findCommentOrThrow(commentId);
        comment.delete();
    }

    private Comment findCommentOrThrow(Long commentId) {
        return commentRepository.findActiveByIdForAdmin(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
    }
}
