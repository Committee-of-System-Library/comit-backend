package kr.ac.knu.comit.post.dto;

import java.util.List;
import kr.ac.knu.comit.post.domain.Post;
import org.springframework.data.domain.Page;

public record AdminPostPageResponse(
        List<AdminPostSummaryResponse> posts,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminPostPageResponse from(Page<Post> postPage) {
        List<AdminPostSummaryResponse> posts = postPage.getContent().stream()
                .map(AdminPostSummaryResponse::from)
                .toList();
        return new AdminPostPageResponse(
                posts,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages()
        );
    }
}
