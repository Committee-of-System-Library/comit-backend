package kr.ac.knu.comit.notice.service;

import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.infrastructure.rag.NoticeRagPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeChatService {

    private final NoticeRagPipeline noticeRagPipeline;

    public NoticeChatResponse chat(String message) {
        NoticeRagPipeline.ChatResult result = noticeRagPipeline.chat(message);
        return NoticeChatResponse.of(result.answer(), result.sources());
    }
}
