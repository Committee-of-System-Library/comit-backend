package kr.ac.knu.comit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 1, max = 15, message = "닉네임은 1~15자이어야 합니다.")
        String nickname,

        @NotBlank(message = "연락처를 입력해주세요.")
        @Pattern(regexp = "^[0-9\\-]{10,15}$", message = "연락처는 숫자와 하이픈만 포함한 10~15자여야 합니다.")
        String phone,

        String profileImageUrl,

        boolean agreedToTerms
) {
}
