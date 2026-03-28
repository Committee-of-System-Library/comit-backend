package kr.ac.knu.comit.report.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateReportRequest(
        @NotBlank
        String message
) {
}
