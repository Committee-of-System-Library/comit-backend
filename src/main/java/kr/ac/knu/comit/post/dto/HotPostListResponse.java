package kr.ac.knu.comit.post.dto;

import kr.ac.knu.comit.post.domain.Post;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record HotPostListResponse(
        List<HotPostResponse> posts
) {
    public static HotPostListResponse empty() {
        return new HotPostListResponse(List.of());
    }

    public static HotPostListResponse of(List<Post> posts, List<Long> orderedPostIds, Map<Long, Integer> commentCounts) {
        Map<Long, Integer> rankByPostId = java.util.stream.IntStream.range(0, orderedPostIds.size())
                .boxed()
                .collect(Collectors.toMap(orderedPostIds::get, index -> index + 1));

        List<HotPostResponse> responses = posts.stream()
                .filter(post -> rankByPostId.containsKey(post.getId()))
                .sorted(Comparator.comparingInt(post -> rankByPostId.get(post.getId())))
                .map(post -> HotPostResponse.from(
                        post,
                        rankByPostId.get(post.getId()),
                        commentCounts.getOrDefault(post.getId(), 0)
                ))
                .toList();

        return new HotPostListResponse(responses);
    }
}
