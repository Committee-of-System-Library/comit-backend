package kr.ac.knu.comit.nightsnack.controller.api;

import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/night-snacks")
public interface NightSnackApplicationControllerApi {

    @ApiDoc(
            summary = "야식 마차 선착순 신청",
            description = "지정한 야식 마차에 선착순 신청합니다. 신청 성공 시 QR 수령 티켓 토큰과 신청 순번을 반환합니다. "
                    + "동일 야식 마차에 1인 1회만 신청할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "nightSnackId", value = "신청할 야식 마차 ID입니다."),
                    @FieldDesc(name = "ticketToken", value = "QR 수령 티켓 값입니다."),
                    @FieldDesc(name = "sequence", value = "몇 번째 신청자인지를 나타내는 순번입니다."),
                    @FieldDesc(name = "remaining", value = "응답 시점의 잔여 수량입니다.")
            },
            errors = {
                    @ApiError(code = "EVENT_NOT_FOUND", when = "존재하지 않는 야식 마차 ID로 요청할 때"),
                    @ApiError(code = "EVENT_NOT_OPEN", when = "아직 오픈되지 않았거나 마감된 야식 마차에 신청할 때"),
                    @ApiError(code = "EVENT_SOLD_OUT", when = "정원이 모두 소진되어 마감되었을 때"),
                    @ApiError(code = "ALREADY_APPLIED", when = "이미 신청한 야식 마차에 다시 신청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "ticketToken": "9f1c2e4a-7b8d-4c2a-9e3f-1a2b3c4d5e6f",
                                "sequence": 37,
                                "remaining": 63
                              }
                            }
                            """
            )
    )
    @PostMapping("/{nightSnackId}/applications")
    ResponseEntity<ApiResponse<ApplyResponse>> apply(
            @PathVariable Long nightSnackId,
            @AuthenticatedMember MemberPrincipal principal
    );
}
