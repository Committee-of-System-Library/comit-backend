package kr.ac.knu.comit.member.dto;

import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostLike;

import java.util.List;
import java.util.Map;

public record MyLikedPostCursorPageResponse(
        List<MyLikedPostSummary> posts,
        Long nextCursorId,
        boolean hasNext
) {
    public static MyLikedPostCursorPageResponse of(
            List<PostLike> likes, Map<Long, Post> postById, int requestedSize) {
        boolean hasNext = likes.size() > requestedSize;
        List<PostLike> visible = hasNext ? likes.subList(0, requestedSize) : likes;
        Long nextCursorId = hasNext ? visible.get(visible.size() - 1).getId() : null;
        return new MyLikedPostCursorPageResponse(
                visible.stream()
                        .filter(like -> postById.containsKey(like.getPostId()))
                        .map(like -> MyLikedPostSummary.from(like, postById.get(like.getPostId())))
                        .toList(),
                nextCursorId,
                hasNext
        );
    }
}
