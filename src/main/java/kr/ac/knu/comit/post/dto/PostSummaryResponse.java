package kr.ac.knu.comit.post.dto;

import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 목록 조회 전용 DTO. 본문인 {@code content}는 제외해 페이로드를 최소화한다.
 */
public record PostSummaryResponse(
        Long id,
        BoardType boardType,
        String title,
        String contentPreview,
        String authorNickname,
        int likeCount,
        int commentCount,
        List<String> tags,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post post, int commentCount, List<String> imageUrls,
                                           String contentPreview) {
        return new PostSummaryResponse(
                post.getId(),
                post.getBoardType(),
                post.getTitle(),
                contentPreview,
                post.getMember().getDisplayNickname(),
                post.getLikeCount(),
                commentCount,
                post.getTags().stream().map(t -> t.getName()).toList(),
                imageUrls,
                post.getCreatedAt()
        );
    }
}
