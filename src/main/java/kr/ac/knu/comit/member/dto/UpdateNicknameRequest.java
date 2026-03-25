package kr.ac.knu.comit.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 1, max = 15, message = "닉네임은 1~15자이어야 합니다.")
        String nickname
) {
}
