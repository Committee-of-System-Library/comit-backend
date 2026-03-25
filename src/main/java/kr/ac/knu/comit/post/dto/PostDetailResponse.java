package kr.ac.knu.comit.post.dto;

import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long id,
        BoardType boardType,
        String title,
        String content,
        String authorNickname,
        int likeCount,
        int viewCount,
        boolean likedByMe,
        List<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse of(Post post, boolean likedByMe) {
        return new PostDetailResponse(
                post.getId(),
                post.getBoardType(),
                post.getTitle(),
                post.getContent(),
                post.getMember().getNickname(),
                post.getLikeCount(),
                post.getViewCount(),
                likedByMe,
                post.getTags().stream().map(t -> t.getName()).toList(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
