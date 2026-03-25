package kr.ac.knu.comit.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.ac.knu.comit.post.domain.PostConstraints;

import java.util.List;

public record UpdatePostRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = PostConstraints.TITLE_MAX_LENGTH,
                message = "제목은 " + PostConstraints.TITLE_MAX_LENGTH + "자 이하이어야 합니다.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = PostConstraints.CONTENT_MAX_LENGTH,
                message = "내용은 " + PostConstraints.CONTENT_MAX_LENGTH + "자 이하이어야 합니다.")
        String content,

        @Size(max = PostConstraints.TAG_MAX_COUNT,
                message = "태그는 최대 " + PostConstraints.TAG_MAX_COUNT + "개까지 입력할 수 있습니다.")
        List<
                @NotBlank(message = "태그는 비어 있을 수 없습니다.")
                @Size(max = PostConstraints.TAG_NAME_MAX_LENGTH,
                        message = "태그는 " + PostConstraints.TAG_NAME_MAX_LENGTH + "자 이하이어야 합니다.")
                String> tags
) {
    public List<String> tags() {
        return tags == null ? List.of() : tags;
    }
}
