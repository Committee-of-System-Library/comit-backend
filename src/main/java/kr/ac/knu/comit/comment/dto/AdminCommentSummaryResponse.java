package kr.ac.knu.comit.comment.dto;

import java.time.LocalDateTime;
import kr.ac.knu.comit.comment.domain.Comment;

public record AdminCommentSummaryResponse(
        Long id,
        Long postId,
        Long parentCommentId,
        String content,
        String authorNickname,
        int helpfulCount,
        boolean hiddenByAdmin,
        LocalDateTime createdAt
) {
    public static AdminCommentSummaryResponse from(Comment comment) {
        return new AdminCommentSummaryResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentCommentId(),
                comment.getContent(),
                comment.getMember().getNickname(),
                comment.getHelpfulCount(),
                comment.isHiddenByAdmin(),
                comment.getCreatedAt()
        );
    }
}
