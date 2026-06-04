package kr.ac.knu.comit.image.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.StorageErrorCode;
import kr.ac.knu.comit.global.storage.S3StorageUploader;
import kr.ac.knu.comit.global.storage.StorageUploader;
import kr.ac.knu.comit.image.dto.PresignedUploadRequest;
import kr.ac.knu.comit.image.dto.PresignedUploadResponse;
import kr.ac.knu.comit.image.dto.UploadImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String SIZE_EXCEEDED_MESSAGE = "이미지 크기는 5MB를 초과할 수 없습니다.";
    private static final String UNSUPPORTED_TYPE_MESSAGE = "지원하지 않는 이미지 형식입니다. (허용: jpg, jpeg, png, webp, gif)";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final StorageUploader storageUploader;
    private final S3StorageUploader s3StorageUploader;

    public UploadImageResponse upload(MultipartFile file, String folder) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(SIZE_EXCEEDED_MESSAGE, StorageErrorCode.FILE_SIZE_EXCEEDED);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(UNSUPPORTED_TYPE_MESSAGE, StorageErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        String url = storageUploader.upload(file, folder);
        return new UploadImageResponse(url);
    }

    public PresignedUploadResponse generatePresignedUrl(PresignedUploadRequest request) {
        if (!ALLOWED_CONTENT_TYPES.contains(request.contentType())) {
            throw new BusinessException(UNSUPPORTED_TYPE_MESSAGE, StorageErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        S3StorageUploader.PresignedUploadUrls urls = s3StorageUploader.generatePresignedUploadUrl(
                request.folder(), request.fileName(), request.contentType()
        );
        return new PresignedUploadResponse(urls.presignedUrl(), urls.imageUrl());
    }
}
