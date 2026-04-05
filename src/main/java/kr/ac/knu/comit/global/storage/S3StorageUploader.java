package kr.ac.knu.comit.global.storage;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.StorageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3StorageUploader implements StorageUploader {

    private static final Duration PRESIGNED_URL_EXPIRY = Duration.ofMinutes(10);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public String upload(MultipartFile file, String folder) {
        String key = folder + "/" + UUID.randomUUID() + extractExtension(file.getOriginalFilename());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.bucketName())
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new BusinessException(StorageErrorCode.FILE_UPLOAD_FAILED);
        }

        return s3Properties.baseUrl() + "/" + key;
    }

    public PresignedUploadUrls generatePresignedUploadUrl(String folder, String fileName, String contentType) {
        String key = folder + "/" + UUID.randomUUID() + extractExtension(fileName);

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(PRESIGNED_URL_EXPIRY)
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(s3Properties.bucketName())
                                .key(key)
                                .contentType(contentType)
                                .build())
                        .build()
        );

        String presignedUrl = presignedRequest.url().toString();
        String imageUrl = s3Properties.baseUrl() + "/" + key;
        return new PresignedUploadUrls(presignedUrl, imageUrl);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public record PresignedUploadUrls(String presignedUrl, String imageUrl) {}
}
