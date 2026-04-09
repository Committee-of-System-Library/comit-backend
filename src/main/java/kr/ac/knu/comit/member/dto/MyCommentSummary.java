package kr.ac.knu.comit.member.dto;

import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.post.domain.BoardType;

import java.time.LocalDateTime;

public record MyCommentSummary(
        Long id,
        String content,
        Long postId,
        String postTitle,
        BoardType boardType,
        LocalDateTime createdAt
) {
    public static MyCommentSummary from(Comment comment) {
        return new MyCommentSummary(
                comment.getId(),
                comment.getContent(),
                comment.getPost().getId(),
                comment.getPost().getTitle(),
                comment.getPost().getBoardType(),
                comment.getCreatedAt()
        );
    }
}
