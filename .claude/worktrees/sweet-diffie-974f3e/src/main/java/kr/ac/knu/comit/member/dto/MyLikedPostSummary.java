package kr.ac.knu.comit.member.dto;

import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostLike;

import java.time.LocalDateTime;

public record MyLikedPostSummary(
        Long postId,
        String postTitle,
        BoardType boardType,
        LocalDateTime likedAt
) {
    public static MyLikedPostSummary from(PostLike like, Post post) {
        return new MyLikedPostSummary(
                post.getId(),
                post.getTitle(),
                post.getBoardType(),
                like.getCreatedAt()
        );
    }
}
