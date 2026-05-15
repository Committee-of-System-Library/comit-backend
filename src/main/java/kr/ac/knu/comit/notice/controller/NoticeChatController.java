package kr.ac.knu.comit.notice.controller;

import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.notice.controller.api.NoticeChatControllerApi;
import kr.ac.knu.comit.notice.dto.NoticeChatRequest;
import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import kr.ac.knu.comit.notice.service.NoticeChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NoticeChatController implements NoticeChatControllerApi {

    private final NoticeChatService noticeChatService;

    @Override
    public ResponseEntity<ApiResponse<NoticeChatResponse>> chat(NoticeChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeChatService.chat(request.message())));
    }
}
