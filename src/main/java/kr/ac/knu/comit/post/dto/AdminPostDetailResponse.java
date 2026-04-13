package kr.ac.knu.comit.post.dto;

import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostImage;
import kr.ac.knu.comit.post.domain.PostTag;
import kr.ac.knu.comit.post.domain.BoardType;

import java.time.LocalDateTime;
import java.util.List;

public record AdminPostDetailResponse(
        Long id,
        BoardType boardType,
        String title,
        String content,
        String authorNickname,
        int likeCount,
        int viewCount,
        boolean hiddenByAdmin,
        List<String> tags,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminPostDetailResponse from(Post post) {
        return new AdminPostDetailResponse(
                post.getId(),
                post.getBoardType(),
                post.getTitle(),
                post.getContent(),
                post.getMember().getDisplayNickname(),
                post.getLikeCount(),
                post.getViewCount(),
                post.isHiddenByAdmin(),
                post.getTags().stream().map(PostTag::getName).toList(),
                post.getImages().stream().map(PostImage::getImageUrl).toList(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
