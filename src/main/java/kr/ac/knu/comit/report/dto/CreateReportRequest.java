package kr.ac.knu.comit.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotBlank
        @Size(max = 500)
        String message
) {
}
