package kr.ac.knu.comit.comment.dto;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knu.comit.comment.domain.Comment;

public record CommentResponse(
        Long id,
        String content,
        String authorNickname,
        int helpfulCount,
        boolean helpfulByMe,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ReplyResponse> replies
) {
    public static CommentResponse from(Comment comment, boolean helpfulByMe, boolean mine, List<ReplyResponse> replies) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getMember().getNickname(),
                comment.getHelpfulCount(),
                helpfulByMe,
                mine,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }
}
