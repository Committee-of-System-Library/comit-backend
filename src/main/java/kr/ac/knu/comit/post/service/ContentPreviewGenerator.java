package kr.ac.knu.comit.post.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게시글 본문 미리보기를 생성한다.
 *
 * <p>빈 줄을 제거한 뒤 최대 {@code maxLines}줄까지 보여주고,
 * 초과 시 마지막에 {@code ...}를 붙인다.
 * 최대 줄 수는 {@code comit.post.preview.max-lines}로 조정한다.
 */
@Component
public class ContentPreviewGenerator {

    private final int maxLines;

    public ContentPreviewGenerator(
            @Value("${comit.post.preview.max-lines:2}") int maxLines) {
        this.maxLines = maxLines;
    }

    public String generate(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        List<String> lines = Arrays.stream(content.split("\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (lines.isEmpty()) {
            return "";
        }

        if (lines.size() <= maxLines) {
            return lines.stream()
                    .collect(Collectors.joining(" "))
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        return lines.subList(0, maxLines).stream()
                .collect(Collectors.joining(" "))
                .replaceAll("\\s+", " ")
                .trim() + "...";
    }
}
