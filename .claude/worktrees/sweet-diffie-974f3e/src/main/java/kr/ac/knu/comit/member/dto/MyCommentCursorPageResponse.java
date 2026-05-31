package kr.ac.knu.comit.member.dto;

import kr.ac.knu.comit.comment.domain.Comment;

import java.util.List;

public record MyCommentCursorPageResponse(
        List<MyCommentSummary> comments,
        Long nextCursorId,
        boolean hasNext
) {
    public static MyCommentCursorPageResponse of(List<Comment> comments, int requestedSize) {
        boolean hasNext = comments.size() > requestedSize;
        List<Comment> visible = hasNext ? comments.subList(0, requestedSize) : comments;
        Long nextCursorId = hasNext ? visible.get(visible.size() - 1).getId() : null;
        return new MyCommentCursorPageResponse(
                visible.stream().map(MyCommentSummary::from).toList(),
                nextCursorId,
                hasNext
        );
    }
}
