package kr.ac.knu.comit.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        Long parentCommentId,

        @NotBlank(message = "댓글 내용을 입력해주세요.")
        String content
) {
}
