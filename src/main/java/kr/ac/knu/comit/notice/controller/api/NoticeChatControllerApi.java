package kr.ac.knu.comit.notice.controller.api;

import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.notice.dto.NoticeChatRequest;
import kr.ac.knu.comit.notice.dto.NoticeChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/official-notices")
public interface NoticeChatControllerApi {

    @ApiDoc(
            summary = "공지사항 챗봇",
            description = "자연어 질문을 받아 저장된 공지사항을 기반으로 AI가 답변합니다.",
            descriptions = {
                    @FieldDesc(name = "message", value = "사용자 질문입니다."),
                    @FieldDesc(name = "answer", value = "공지사항 기반 AI 답변입니다.")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "answer": "2026학년도 1학기 수강신청은 2월 10일부터 시작합니다."
                              }
                            }
                            """
            )
    )
    @PostMapping("/chat")
    ResponseEntity<ApiResponse<NoticeChatResponse>> chat(@RequestBody NoticeChatRequest request);
}
