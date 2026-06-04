package kr.ac.knu.comit.attachment.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedPdfUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotBlank String folder
) {
}