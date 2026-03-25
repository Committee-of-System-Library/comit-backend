package kr.ac.knu.comit.comment.dto;

import java.time.LocalDateTime;
import kr.ac.knu.comit.comment.domain.Comment;

public record ReplyResponse(
        Long id,
        String content,
        String authorNickname,
        int helpfulCount,
        boolean helpfulByMe,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReplyResponse from(Comment comment, boolean helpfulByMe, boolean mine) {
        return new ReplyResponse(
                comment.getId(),
                comment.getContent(),
                comment.getMember().getNickname(),
                comment.getHelpfulCount(),
                helpfulByMe,
                mine,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
