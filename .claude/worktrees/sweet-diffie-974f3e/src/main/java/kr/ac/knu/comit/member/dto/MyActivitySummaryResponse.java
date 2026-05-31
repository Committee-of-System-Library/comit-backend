package kr.ac.knu.comit.member.dto;

import java.util.List;

public record MyActivitySummaryResponse(
        long postCount,
        long commentCount,
        long likeCount,
        List<MyPostSummary> recentPosts,
        List<MyCommentSummary> recentComments,
        List<MyLikedPostSummary> recentLikes
) {
}
