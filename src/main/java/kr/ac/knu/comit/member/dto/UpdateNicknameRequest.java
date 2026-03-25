package kr.ac.knu.comit.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 1, max = 50, message = "닉네임은 1~50자이어야 합니다.")
        String nickname
) {
}
