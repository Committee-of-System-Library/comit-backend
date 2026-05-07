package kr.ac.knu.comit.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateOfficialNoticeRequest(
        @NotBlank @Size(max = 300)
        String title,

        @NotBlank
        String content,

        String author,
        String originalUrl,
        LocalDateTime postedAt
) {
}
