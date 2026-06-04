package kr.ac.knu.comit.attachment.service;

import kr.ac.knu.comit.attachment.domain.AttachmentFolder;
import kr.ac.knu.comit.attachment.domain.PdfPolicy;
import kr.ac.knu.comit.attachment.dto.PresignedPdfUploadRequest;
import kr.ac.knu.comit.attachment.dto.PresignedPdfUploadResponse;
import kr.ac.knu.comit.global.storage.S3StorageUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final S3StorageUploader s3StorageUploader;

    public PresignedPdfUploadResponse generatePresignedUploadUrl(PresignedPdfUploadRequest request) {
        PdfPolicy.requireContentType(request.contentType());
        AttachmentFolder folder = AttachmentFolder.from(request.folder());

        S3StorageUploader.PresignedUploadUrls urls = s3StorageUploader.generatePresignedUploadUrl(
                folder.value(), request.fileName(), request.contentType()
        );
        return new PresignedPdfUploadResponse(urls.presignedUrl(), urls.imageUrl());
    }
}
