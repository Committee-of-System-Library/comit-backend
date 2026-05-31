package kr.ac.knu.comit.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "comit.storage.s3")
public record S3Properties(
        String bucketName,
        String region,
        String accessKey,
        String secretKey,
        String baseUrl
) {
}
