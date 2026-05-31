package kr.ac.knu.comit.member.dto;

import kr.ac.knu.comit.post.domain.Post;

import java.time.LocalDateTime;

public record MyPostSummary(
        Long id,
        String title,
        LocalDateTime createdAt
) {
    public static MyPostSummary from(Post post) {
        return new MyPostSummary(post.getId(), post.getTitle(), post.getCreatedAt());
    }
}
