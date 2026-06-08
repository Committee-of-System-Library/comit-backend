package kr.ac.knu.comit.notice.controller.api;

import jakarta.validation.Valid;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
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

import java.util.concurrent.CompletableFuture;

@ApiContract
@RequestMapping("/official-notices")
public interface NoticeChatControllerApi {

    @ApiDoc(
            summary = "공지사항 챗봇",
            description = "자연어 질문을 받아 저장된 공지사항을 기반으로 AI가 답변합니다.",
            descriptions = {
                    @FieldDesc(name = "message", value = "사용자 질문입니다."),
                    @FieldDesc(name = "answer", value = "공지사항 기반 AI 답변입니다."),
                    @FieldDesc(name = "sources", value = "답변에 사용된 공지 목록입니다. 관련 공지가 없으면 빈 배열입니다."),
                    @FieldDesc(name = "sources[].noticeId", value = "공지 ID입니다."),
                    @FieldDesc(name = "sources[].title", value = "공지 제목입니다."),
                    @FieldDesc(name = "sources[].originalUrl", value = "원문 공지 링크입니다.")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "answer": "2026학년도 1학기 수강신청은 2월 10일부터 시작합니다.",
                                "sources": [
                                  {
                                    "noticeId": 42,
                                    "title": "2026학년도 1학기 수강신청 안내",
                                    "originalUrl": "https://cse.knu.ac.kr/bbs/board.php?bo_table=sub5_1&wr_id=12345"
                                  }
                                ]
                              }
                            }
                            """
            )
    )
    @PostMapping("/chat")
    CompletableFuture<ResponseEntity<ApiResponse<NoticeChatResponse>>> chat(
            @AuthenticatedMember MemberPrincipal principal,
            @Valid @RequestBody NoticeChatRequest request);
}
