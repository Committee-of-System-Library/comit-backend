package kr.ac.knu.comit.comment.dto;

import java.util.List;
import kr.ac.knu.comit.comment.domain.Comment;
import org.springframework.data.domain.Page;

public record AdminCommentPageResponse(
        List<AdminCommentSummaryResponse> comments,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminCommentPageResponse from(Page<Comment> commentPage) {
        List<AdminCommentSummaryResponse> comments = commentPage.getContent().stream()
                .map(AdminCommentSummaryResponse::from)
                .toList();
        return new AdminCommentPageResponse(
                comments,
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getTotalElements(),
                commentPage.getTotalPages()
        );
    }
}
