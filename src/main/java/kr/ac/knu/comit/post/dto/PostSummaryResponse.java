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
    private static final int CONTENT_PREVIEW_MAX_LENGTH = 80;

    public static PostSummaryResponse from(Post post, int commentCount, List<String> imageUrls) {
        return new PostSummaryResponse(
                post.getId(),
                post.getBoardType(),
                post.getTitle(),
                toContentPreview(post.getContent()),
                post.getMember().getNickname(),
                post.getLikeCount(),
                commentCount,
                post.getTags().stream().map(t -> t.getName()).toList(),
                imageUrls,
                post.getCreatedAt()
        );
    }

    private static String toContentPreview(String content) {
        // TODO: preview 로직 복잡화 시 ContentPreviewGenerator 분리 검토
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= CONTENT_PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, CONTENT_PREVIEW_MAX_LENGTH) + "...";
    }
}
