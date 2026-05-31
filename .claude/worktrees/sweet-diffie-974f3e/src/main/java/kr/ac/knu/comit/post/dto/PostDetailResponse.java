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
        String authorProfileImageUrl,
        int likeCount,
        int viewCount,
        boolean likedByMe,
        List<String> tags,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse of(Post post, boolean likedByMe, List<String> imageUrls) {
        return new PostDetailResponse(
                post.getId(),
                post.getBoardType(),
                post.getTitle(),
                post.getContent(),
                post.getMember().getDisplayNickname(),
                post.getMember().getProfileImageUrl(),
                post.getLikeCount(),
                post.getViewCount(),
                likedByMe,
                post.getTags().stream().map(t -> t.getName()).toList(),
                imageUrls,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
