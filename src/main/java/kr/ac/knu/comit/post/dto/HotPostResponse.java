package kr.ac.knu.comit.post.dto;

import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;

import java.time.LocalDateTime;
import java.util.List;

public record HotPostResponse(
        int rank,
        Long id,
        BoardType boardType,
        String title,
        String authorNickname,
        int likeCount,
        int commentCount,
        List<String> tags,
        LocalDateTime createdAt
) {
    public static HotPostResponse from(Post post, int rank, int commentCount) {
        return new HotPostResponse(
                rank,
                post.getId(),
                post.getBoardType(),
                post.getTitle(),
                post.getMember().getDisplayNickname(),
                post.getLikeCount(),
                commentCount,
                post.getTags().stream().map(tag -> tag.getName()).toList(),
                post.getCreatedAt()
        );
    }
}
