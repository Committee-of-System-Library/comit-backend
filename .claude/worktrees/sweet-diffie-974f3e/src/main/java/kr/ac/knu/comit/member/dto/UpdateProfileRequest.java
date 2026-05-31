package kr.ac.knu.comit.member.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 15, message = "닉네임은 1~15자이어야 합니다.")
        String nickname,

        @Pattern(regexp = "^[0-9\\-]{10,15}$", message = "연락처는 숫자와 하이픈만 포함한 10~15자이어야 합니다.")
        String phone,

        String profileImageUrl
) {
}
