package kr.ac.knu.comit.comment.dto;

import java.time.LocalDateTime;
import kr.ac.knu.comit.comment.domain.Comment;

public record ReplyResponse(
        Long id,
        String content,
        String authorNickname,
        int likeCount,
        boolean likedByMe,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReplyResponse from(Comment comment, boolean likedByMe, boolean mine) {
        return new ReplyResponse(
                comment.getId(),
                comment.getContent(),
                comment.getMember().getNickname(),
                comment.getLikeCount(),
                likedByMe,
                mine,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
