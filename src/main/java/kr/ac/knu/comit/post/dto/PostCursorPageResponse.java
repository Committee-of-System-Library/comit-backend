package kr.ac.knu.comit.post.dto;

import kr.ac.knu.comit.post.domain.Post;
import java.util.List;
import java.util.Map;

/**
 * no-offset cursor 페이지네이션 응답.
 *
 * 클라이언트는 {@code nextCursorId}를 다음 요청의 {@code cursor} 파라미터로 전달한다.
 * {@code nextCursorId == null}이면 마지막 페이지다.
 */
public record PostCursorPageResponse(
        List<PostSummaryResponse> posts,
        Long nextCursorId,
        boolean hasNext
) {
    public static PostCursorPageResponse of(List<Post> posts, int requestedSize, Map<Long, Integer> commentCounts) {
        boolean hasNext = posts.size() > requestedSize;
        List<Post> visiblePosts = hasNext ? posts.subList(0, requestedSize) : posts;
        Long nextCursorId = hasNext ? visiblePosts.get(visiblePosts.size() - 1).getId() : null;
        return new PostCursorPageResponse(
                visiblePosts.stream()
                        .map(post -> PostSummaryResponse.from(post, commentCounts.getOrDefault(post.getId(), 0)))
                        .toList(),
                nextCursorId,
                hasNext
        );
    }
}
