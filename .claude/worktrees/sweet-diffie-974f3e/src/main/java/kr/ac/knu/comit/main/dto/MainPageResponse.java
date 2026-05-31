package kr.ac.knu.comit.main.dto;

import kr.ac.knu.comit.main.service.MainPageSections;
import kr.ac.knu.comit.post.dto.HotPostResponse;
import kr.ac.knu.comit.post.dto.PostSummaryResponse;

import java.util.List;
import java.util.Objects;

public record MainPageResponse(
        List<PostSummaryResponse> qna,
        List<PostSummaryResponse> info,
        List<PostSummaryResponse> free,
        List<PostSummaryResponse> notice,
        List<PostSummaryResponse> event,
        List<HotPostResponse> hotPosts
) {
    public MainPageResponse {
        qna = immutableList(qna);
        info = immutableList(info);
        free = immutableList(free);
        notice = immutableList(notice);
        event = immutableList(event);
        hotPosts = immutableList(hotPosts);
    }

    public static MainPageResponse of(MainPageSections sections, List<HotPostResponse> hotPosts) {
        return new MainPageResponse(
                sections.qna(),
                sections.info(),
                sections.free(),
                sections.notice(),
                sections.event(),
                hotPosts
        );
    }

    private static <T> List<T> immutableList(List<T> items) {
        return List.copyOf(Objects.requireNonNullElse(items, List.of()));
    }
}
