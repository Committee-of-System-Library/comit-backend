package kr.ac.knu.comit.image.dto;

public record PresignedUploadResponse(
        String presignedUrl,
        String imageUrl
) {
}
