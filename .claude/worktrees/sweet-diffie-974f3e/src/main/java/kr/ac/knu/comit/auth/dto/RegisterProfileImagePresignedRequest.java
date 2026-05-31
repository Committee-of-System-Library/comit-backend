package kr.ac.knu.comit.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterProfileImagePresignedRequest(
        @NotBlank(message = "파일 이름을 입력해주세요.")
        String fileName,

        @NotBlank(message = "파일 형식을 입력해주세요.")
        String contentType
) {
}
