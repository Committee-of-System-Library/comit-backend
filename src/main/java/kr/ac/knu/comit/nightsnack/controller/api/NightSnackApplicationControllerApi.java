package kr.ac.knu.comit.nightsnack.controller.api;

import java.time.LocalDate;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.nightsnack.dto.MyTicketResponse;
import kr.ac.knu.comit.nightsnack.dto.NightSnackResponse;
import kr.ac.knu.comit.nightsnack.dto.StudentCouncilFeeResponse;
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
            summary = "다가오는 야식 마차 단건 조회",
            description = "기준 날짜(yyyy-MM-dd) 당일을 포함해 그 이후로 가장 가까운 야식 마차 1건을 조회합니다. "
                    + "당일에 야식 마차가 있으면 그것을, 없으면 다음 예정 야식 마차를 반환합니다. "
                    + "인증 없이 호출 가능합니다. openAt을 이용해 클라이언트 카운트다운 타이머를 구성할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "date", value = "조회 기준 날짜입니다. 이 날짜 당일부터 이후로 가장 가까운 야식 마차를 찾습니다. (yyyy-MM-dd)"),
                    @FieldDesc(name = "nightSnackId", value = "야식 마차 ID입니다."),
                    @FieldDesc(name = "status", value = "야식 마차 상태입니다. (SCHEDULED / OPEN / CLOSED)"),
                    @FieldDesc(name = "openAt", value = "신청 오픈 시각입니다. 클라이언트 카운트다운 기준으로 사용합니다."),
                    @FieldDesc(name = "closeAt", value = "신청 마감 시각입니다."),
                    @FieldDesc(name = "requiresStudentCouncilFee", value = "학생회비 납부 여부 신청자격입니다.")
            },
            errors = {
                    @ApiError(code = "EVENT_NOT_FOUND", when = "기준 날짜 이후로 예정된 야식 마차가 하나도 없을 때")
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
                    + "selectionStatus가 PENDING이면 이 API를 폴링하고, ACCEPTED일 때만 ticketToken을 QR로 표시합니다. REJECTED이면 탈락입니다.",
            descriptions = {
                    @FieldDesc(name = "nightSnackId", value = "조회할 야식 마차 ID입니다."),
                    @FieldDesc(name = "applicationId", value = "신청 ID입니다."),
                    @FieldDesc(name = "ticketToken", value = "ACCEPTED일 때만 제공하는 QR 티켓입니다. 나머지는 null입니다."),
                    @FieldDesc(name = "selectionStatus", value = "신청 당락입니다. PENDING / ACCEPTED / REJECTED. status(수령 상태)와 별개입니다."),
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
                                "selectionStatus": "ACCEPTED",
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
            description = "지정한 야식 마차에 신청을 접수합니다. 일반 정원의 최대 50%는 즉시 ACCEPTED, 이후는 PENDING입니다. 스케줄러가 마감 후 ID 순으로 최종 선정합니다. "
                    + "동일 야식 마차에 1인 1회만 신청할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "nightSnackId", value = "신청할 야식 마차 ID입니다."),
                    @FieldDesc(name = "ticketToken", value = "ACCEPTED일 때만 제공하는 QR 티켓입니다. 나머지는 null입니다."),
                    @FieldDesc(name = "selectionStatus", value = "신청 당락입니다. PENDING / ACCEPTED / REJECTED. status(수령 상태)와 별개입니다."),
                    @FieldDesc(name = "applicationId", value = "접수된 신청 ID입니다. 내 티켓 조회로 최종 결과를 확인합니다.")
            },
            errors = {
                    @ApiError(code = "EVENT_NOT_FOUND", when = "존재하지 않는 야식 마차 ID로 요청할 때"),
                    @ApiError(code = "EVENT_NOT_OPEN", when = "아직 오픈되지 않았거나 마감된 야식 마차에 신청할 때"),
                    @ApiError(code = "EVENT_SOLD_OUT", when = "일반 신청 정원이 0명일 때"),
                    @ApiError(code = "ALREADY_APPLIED", when = "이미 신청한 야식 마차에 다시 신청할 때"),
                    @ApiError(code = "STUDENT_COUNCIL_FEE_REQUIRED", when = "학생회비 납부자 전용 야식 마차에서 로그인 회원의 학번이 납부 상태가 아닐 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "applicationId": 42,
                                "selectionStatus": "PENDING",
                                "ticketToken": null
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

    @ApiDoc(
            summary = "학생회비 납부 여부 조회",
            description = "로그인한 회원의 학번으로 학생회비 납부 여부를 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "paid", value = "학생회비 납부 여부입니다. is_paid=1이면 true, 미납 또는 납부 행이 없으면 false를 반환합니다.")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": { "paid": true }
                            }
                            """
            )
    )
    @GetMapping("/student-council-fee")
    ResponseEntity<ApiResponse<StudentCouncilFeeResponse>> getStudentCouncilFeeStatus(
            @AuthenticatedMember MemberPrincipal principal
    );
}
