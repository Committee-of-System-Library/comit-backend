package kr.ac.knu.comit.notice.service;

import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.infra.NoticeChatClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeChatService {

    private final NoticeChatClient noticeChatClient;

    public NoticeChatResponse chat(String message) {
        NoticeChatClient.ChatResult result = noticeChatClient.chat(message);
        return NoticeChatResponse.of(result.answer(), result.sources());
    }
}
