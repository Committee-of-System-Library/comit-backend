package kr.ac.knu.comit.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInquiryRequest(
        @NotBlank @Size(max = 30) String title,
        @NotBlank @Size(max = 500) String content
) {
}
