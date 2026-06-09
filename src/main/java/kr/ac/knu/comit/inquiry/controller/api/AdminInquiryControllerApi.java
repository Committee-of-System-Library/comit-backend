package kr.ac.knu.comit.inquiry.controller.api;

import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.inquiry.dto.AdminInquiryPageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/admin/inquiries")
public interface AdminInquiryControllerApi {

    @ApiDoc(
            summary = "문의 목록 조회",
            description = "관리자가 전체 문의 목록을 최신순으로 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "inquiries", value = "문의 목록"),
                    @FieldDesc(name = "page", value = "현재 페이지 번호. 0부터 시작합니다."),
                    @FieldDesc(name = "size", value = "페이지 크기"),
                    @FieldDesc(name = "totalElements", value = "전체 문의 수"),
                    @FieldDesc(name = "totalPages", value = "전체 페이지 수")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "inquiries": [
                                  {
                                    "id": 1,
                                    "title": "로그인이 안 됩니다",
                                    "content": "어제부터 로그인 시도 시 오류가 발생합니다.",
                                    "memberNickname": "comit-user",
                                    "createdAt": "2026-06-09T10:00:00"
                                  }
                                ],
                                "page": 0,
                                "size": 20,
                                "totalElements": 1,
                                "totalPages": 1
                              }
                            }
                            """
            )
    )
    @GetMapping
    ResponseEntity<ApiResponse<AdminInquiryPageResponse>> getInquiries(
            Pageable pageable,
            @AuthenticatedMember MemberPrincipal principal
    );
}
