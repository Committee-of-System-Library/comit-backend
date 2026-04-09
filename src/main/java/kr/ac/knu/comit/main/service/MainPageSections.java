package kr.ac.knu.comit.main.service;

import kr.ac.knu.comit.post.dto.PostSummaryResponse;

import java.util.List;
import java.util.Objects;

public record MainPageSections(
        List<PostSummaryResponse> qna,
        List<PostSummaryResponse> info,
        List<PostSummaryResponse> free,
        List<PostSummaryResponse> notice,
        List<PostSummaryResponse> event
) {
    public MainPageSections {
        qna = immutableList(qna);
        info = immutableList(info);
        free = immutableList(free);
        notice = immutableList(notice);
        event = immutableList(event);
    }

    private static <T> List<T> immutableList(List<T> items) {
        return List.copyOf(Objects.requireNonNullElse(items, List.of()));
    }
}
