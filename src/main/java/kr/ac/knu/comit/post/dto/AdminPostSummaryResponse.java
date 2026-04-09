package kr.ac.knu.comit.post.dto;

import java.time.LocalDateTime;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;

public record AdminPostSummaryResponse(
        Long id,
        BoardType boardType,
        String title,
        String authorNickname,
        int likeCount,
        int viewCount,
        boolean hiddenByAdmin,
        LocalDateTime createdAt
) {
    public static AdminPostSummaryResponse from(Post post) {
        return new AdminPostSummaryResponse(
                post.getId(),
                post.getBoardType(),
                post.getTitle(),
                post.getMember().getDisplayNickname(),
                post.getLikeCount(),
                post.getViewCount(),
                post.isHiddenByAdmin(),
                post.getCreatedAt()
        );
    }
}
