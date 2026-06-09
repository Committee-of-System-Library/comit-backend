package kr.ac.knu.comit.inquiry.controller.api;

import jakarta.validation.Valid;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.inquiry.dto.CreateInquiryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/inquiries")
public interface InquiryControllerApi {

    @ApiDoc(
            summary = "문의 등록",
            description = "로그인한 회원이 문의를 등록합니다. 제목은 30자, 내용은 500자 이하여야 합니다.",
            descriptions = {
                    @FieldDesc(name = "title", value = "문의 제목. 1자 이상 30자 이하."),
                    @FieldDesc(name = "content", value = "문의 내용. 1자 이상 500자 이하.")
            },
            errors = {
                    @ApiError(code = "MEMBER_NOT_FOUND", when = "인증된 사용자의 로컬 회원 정보가 존재하지 않을 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "제목 또는 내용이 비어있거나 길이 제한을 초과할 때")
            },
            example = @Example(
                    request = """
                            {
                              "title": "로그인이 안 됩니다",
                              "content": "어제부터 로그인 시도 시 오류가 발생합니다."
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PostMapping
    ResponseEntity<ApiResponse<Void>> create(
            @RequestBody @Valid CreateInquiryRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );
}
