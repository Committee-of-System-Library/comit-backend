package kr.ac.knu.comit.post.dto;

import kr.ac.knu.comit.post.domain.Post;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record PostSearchPageResponse(
        long totalCount,
        boolean hasNext,
        Long nextCursorId,
        List<PostSummaryResponse> posts
) {
    public static PostSearchPageResponse of(List<Post> posts, int requestedSize, long totalCount,
                                            Map<Long, Integer> commentCounts,
                                            Map<Long, List<String>> imageUrlsByPostId,
                                            Function<String, String> previewFn) {
        boolean hasNext = posts.size() > requestedSize;
        List<Post> visiblePosts = hasNext ? posts.subList(0, requestedSize) : posts;
        Long nextCursorId = hasNext ? visiblePosts.get(visiblePosts.size() - 1).getId() : null;
        return new PostSearchPageResponse(
                totalCount,
                hasNext,
                nextCursorId,
                visiblePosts.stream()
                        .map(post -> PostSummaryResponse.from(
                                post,
                                commentCounts.getOrDefault(post.getId(), 0),
                                imageUrlsByPostId.getOrDefault(post.getId(), Collections.emptyList()),
                                previewFn.apply(post.getContent())
                        ))
                        .toList()
        );
    }
}
