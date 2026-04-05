package kr.ac.knu.comit.image.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotBlank String folder
) {
}
