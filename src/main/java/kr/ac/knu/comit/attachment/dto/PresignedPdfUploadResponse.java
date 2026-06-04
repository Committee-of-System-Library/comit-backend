package kr.ac.knu.comit.attachment.dto;

public record PresignedPdfUploadResponse(
        String presignedUrl,
        String fileUrl
) {
}