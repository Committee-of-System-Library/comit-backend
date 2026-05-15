package kr.ac.knu.comit.notice.dto;

import java.util.List;

public record NoticeChatResponse(
        String answer,
        List<NoticeSource> sources
) {
    public static NoticeChatResponse of(String answer, List<NoticeSource> sources) {
        return new NoticeChatResponse(answer, sources);
    }
}
