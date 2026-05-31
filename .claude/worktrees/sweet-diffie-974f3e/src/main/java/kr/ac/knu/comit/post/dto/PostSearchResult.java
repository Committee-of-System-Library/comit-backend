package kr.ac.knu.comit.post.dto;

import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;

public record PostSearchResult(
        Long id,
        BoardType boardType,
        String title,
        String contentPreview
) {
    private static final int PREVIEW_MAX_LENGTH = 60;

    public static PostSearchResult from(Post post) {
        String content = post.getContent();
        String preview = content.length() > PREVIEW_MAX_LENGTH
                ? content.substring(0, PREVIEW_MAX_LENGTH) + "..."
                : content;
        return new PostSearchResult(post.getId(), post.getBoardType(), post.getTitle(), preview);
    }
}
