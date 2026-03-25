package kr.ac.knu.comit.comment.dto;

import java.util.List;

public record CommentListResponse(List<CommentResponse> comments) {
}
