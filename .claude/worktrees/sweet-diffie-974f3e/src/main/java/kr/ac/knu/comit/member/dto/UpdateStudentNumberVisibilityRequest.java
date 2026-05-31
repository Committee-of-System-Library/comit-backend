package kr.ac.knu.comit.member.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStudentNumberVisibilityRequest(
        @NotNull(message = "공개 여부를 입력해주세요.")
        Boolean visible
) {
}
