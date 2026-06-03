package kr.ac.knu.comit.nightsnack.controller.api;

import java.time.LocalDate;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.nightsnack.dto.MyTicketResponse;
import kr.ac.knu.comit.nightsnack.dto.NightSnackResponse;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;



@ApiContract
@RequestMapping("/night-snacks")
public interface NightSnackApplicationControllerApi {

    @ApiDoc(
            summary = "날짜별 야식 마차 단건 조회",
            description = "날짜(yyyy-MM-dd)로 야식 마차 정보를 조회합니다. 인증 없이 호출 가능합니다. "
                    + "openAt을 이용해 클라이언트 카운트다운 타이머를 구성할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "date", value = "조회할 야식 마차 날짜입니다. (yyyy-MM-dd)"),
                    @FieldDesc(name = "nightSnackId", value = "야식 마차 ID입니다."),
                    @FieldDesc(name = "status", value = "야식 마차 상태입니다. (SCHEDULED / OPEN / CLOSED)"),
                    @FieldDesc(name = "openAt", value = "신청 오픈 시각입니다. 클라이언트 카운트다운 기준으로 사용합니다."),
                    @FieldDesc(name = "closeAt", value = "신청 마감 시각입니다."),
                    @FieldDesc(name = "requiresStudentCouncilFee", value = "학생회비 납부 여부 신청자격입니다.")
            },
            errors = {
                    @ApiError(code = "EVENT_NOT_FOUND", when = "해당 날짜의 야식 마차가 존재하지 않을 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "nightSnackId": 11,
                                "nightSnackDate": "2026-06-03",
                                "status": "SCHEDULED",
                                "capacity": 100,
                                "remaining": 90,
                                "reservedCapacity": 10,
                                "reservedRemaining": 10,
                                "openAt": "2026-06-03T17:30:00",
                                "closeAt": "2026-06-03T18:30:00",
                                "title": "6월 야식마차",
                                "contents": "선착순 100명에게 야식을 제공합니다.",
                                "menu": "떡볶이, 순대, 튀김",
                                "pickupLocation": "학생회관 1층 로비",
                                "pickupDeadline": "2026-06-03T18:30:00",
                                "requiresStudentCouncilFee": false
                              }
                            }
                            """
            )
    )
    @GetMapping
    ResponseEntity<ApiResponse<NightSnackResponse>> getNightSnack(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );

    @ApiDoc(
            summary = "내 티켓 조회",
            description = "로그인한 사용자가 지정한 야식 마차에 신청한 티켓 정보를 조회합니다. "
                    + "ticketToken을 QR 코드로 렌더링해 수령 시 제시합니다.",
            descriptions = {
                    @FieldDesc(name = "nightSnackId", value = "조회할 야식 마차 ID입니다."),
                    @FieldDesc(name = "applicationId", value = "신청 ID입니다."),
                    @FieldDesc(name = "ticketToken", value = "QR 수령 티켓 값입니다."),
                    @FieldDesc(name = "status", value = "티켓 상태입니다. (PENDING / CONFIRMED / NO_SHOW)"),
                    @FieldDesc(name = "nightSnackDate", value = "야식 마차 날짜입니다."),
                    @FieldDesc(name = "menu", value = "제공 메뉴입니다."),
                    @FieldDesc(name = "pickupLocation", value = "수령 장소입니다."),
                    @FieldDesc(name = "pickupDeadline", value = "수령 마감 시각입니다."),
                    @FieldDesc(name = "appliedAt", value = "신청 시각입니다."),
                    @FieldDesc(name = "confirmedAt", value = "수령 확인 시각입니다. 미수령 시 null입니다.")
            },
            errors = {
                    @ApiError(code = "APPLICATION_NOT_FOUND", when = "해당 야식 마차에 신청 내역이 없을 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "applicationId": 42,
                                "ticketToken": "9f1c2e4a-7b8d-4c2a-9e3f-1a2b3c4d5e6f",
                                "status": "PENDING",
                                "nightSnackDate": "2026-06-03",
                                "menu": "떡볶이, 순대, 튀김",
                                "pickupLocation": "학생회관 1층 로비",
                                "pickupDeadline": "2026-06-03T18:30:00",
                                "appliedAt": "2026-06-03T17:30:01.234",
                                "confirmedAt": null
                              }
                            }
                            """
            )
    )
    @GetMapping("/{nightSnackId}/my-ticket")
    ResponseEntity<ApiResponse<MyTicketResponse>> getMyTicket(
            @PathVariable Long nightSnackId,
            @AuthenticatedMember MemberPrincipal principal
    );

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
