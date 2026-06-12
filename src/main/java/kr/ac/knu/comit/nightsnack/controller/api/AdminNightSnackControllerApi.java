package kr.ac.knu.comit.nightsnack.controller.api;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knu.comit.nightsnack.dto.ApplicationCheckResponse;
import kr.ac.knu.comit.nightsnack.dto.NightSnackResponse;
import kr.ac.knu.comit.nightsnack.dto.CreateNightSnackRequest;
import kr.ac.knu.comit.nightsnack.dto.CreateNightSnackResponse;
import kr.ac.knu.comit.nightsnack.dto.ReserveRequest;
import kr.ac.knu.comit.nightsnack.dto.ReserveResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/admin/night-snacks")
public interface AdminNightSnackControllerApi {

    @ApiDoc(
            summary = "야식 마차 생성",
            description = "관리자가 선착순 야식 마차를 생성합니다. 생성 직후 상태는 SCHEDULED이며, 별도 오픈 요청으로 신청을 받기 시작합니다. "
                    + "사전신청 예약분은 전체 정원의 10%(서버 정책)로 자동 계산됩니다.",
            descriptions = {
                    @FieldDesc(name = "nightSnackDate", value = "야식 마차 날짜입니다. (yyyy-MM-dd)"),
                    @FieldDesc(name = "capacity", value = "전체 정원입니다. 1 이상의 정수입니다."),
                    @FieldDesc(name = "openAt", value = "신청 오픈 시각입니다. (yyyy-MM-ddTHH:mm:ss)"),
                    @FieldDesc(name = "closeAt", value = "신청 마감 시각입니다. openAt 이후여야 합니다. (yyyy-MM-ddTHH:mm:ss)"),
                    @FieldDesc(name = "title", value = "야식 마차 제목입니다. 생략 가능합니다. (최대 200자)"),
                    @FieldDesc(name = "contents", value = "야식 마차 안내 내용입니다. 생략 가능합니다."),
                    @FieldDesc(name = "menu", value = "제공 메뉴입니다. 생략 가능합니다. (최대 500자, 예: 떡볶이, 순대)"),
                    @FieldDesc(name = "pickupLocation", value = "수령 장소입니다. 생략 가능합니다. (최대 200자)"),
                    @FieldDesc(name = "pickupDeadline", value = "수령 마감 시각입니다. 신청 마감(closeAt)과 별개로 현장 수령 가능 시각 상한입니다. 생략 가능합니다. (yyyy-MM-ddTHH:mm:ss)"),
                    @FieldDesc(name = "requiresStudentCouncilFee", value = "신청자격 — 학생회비 납부 여부입니다. 생략 시 false(자격 제한 없음)입니다."),
                    @FieldDesc(name = "nightSnackId", value = "생성된 야식 마차 ID입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "날짜가 없거나 정원이 1 미만일 때")
            },
            example = @Example(
                    request = """
                            {
                              "nightSnackDate": "2026-05-20",
                              "capacity": 100,
                              "openAt": "2026-05-20T17:30:00",
                              "closeAt": "2026-05-20T18:30:00",
                              "title": "5월 야식마차",
                              "contents": "선착순 100명에게 야식을 제공합니다.",
                              "menu": "떡볶이, 순대, 튀김",
                              "pickupLocation": "학생회관 1층 로비",
                              "pickupDeadline": "2026-05-20T18:30:00",
                              "requiresStudentCouncilFee": true
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": { "nightSnackId": 1 }
                            }
                            """
            )
    )
    @PostMapping
    ResponseEntity<ApiResponse<CreateNightSnackResponse>> createNightSnack(
            @RequestBody @Valid CreateNightSnackRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "야식 마차 신청 오픈",
            description = "SCHEDULED 상태의 야식 마차를 OPEN으로 전환해 선착순 신청을 받기 시작합니다. "
                    + "오픈 트리거를 관리자 수동 토글로 둘지 17:30 스케줄러로 둘지는 미확정입니다.",
            descriptions = {
                    @FieldDesc(name = "nightSnackId", value = "오픈할 야식 마차 ID입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "EVENT_NOT_FOUND", when = "존재하지 않는 야식 마차 ID로 요청할 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "SCHEDULED 상태가 아닌 야식 마차를 오픈하려 할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PatchMapping("/{nightSnackId}/open")
    ResponseEntity<ApiResponse<Void>> openNightSnack(
            @PathVariable Long nightSnackId,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "사전신청(예약분) 등록",
            description = "관리자가 학번 목록을 받아 예약분에 사전신청을 미리 등록합니다(요구사항 4-3). "
                    + "Comit 계정이 없는 학번(편입생·초과학기 등)도 등록할 수 있습니다. "
                    + "멱등하게 동작하여 이미 등록된 학번은 건너뛰고, 신규 학번만 예약 잔여분에서 차감합니다. "
                    + "신규 학번 수가 예약 잔여분을 넘기면 전체가 거부됩니다. "
                    + "선착순 오픈 전(SCHEDULED) 상태에서만 등록할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "date", value = "사전신청을 등록할 야식 마차 날짜입니다. (yyyy-MM-dd)"),
                    @FieldDesc(name = "studentNumbers", value = "등록할 학번 목록입니다. 공백/중복은 자동 정리됩니다."),
                    @FieldDesc(name = "requested", value = "중복 제거 후 요청된 학번 수입니다."),
                    @FieldDesc(name = "registered", value = "이번에 신규로 등록된 수입니다."),
                    @FieldDesc(name = "skipped", value = "이미 등록되어 건너뛴 수입니다."),
                    @FieldDesc(name = "tickets", value = "신규 등록된 사전신청자의 티켓 목록입니다."),
                    @FieldDesc(name = "studentNumber", value = "사전신청자의 학번입니다."),
                    @FieldDesc(name = "ticketToken", value = "QR 수령 티켓 값입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "EVENT_NOT_FOUND", when = "해당 날짜의 야식 마차가 존재하지 않을 때"),
                    @ApiError(code = "RESERVATION_NOT_ALLOWED", when = "오픈 전(SCHEDULED) 상태가 아닌 야식 마차에 사전신청할 때"),
                    @ApiError(code = "RESERVED_CAPACITY_EXCEEDED", when = "신규 학번 수가 예약 잔여분을 초과할 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "학번 목록이 비어 있거나 유효한 학번이 하나도 없을 때")
            },
            example = @Example(
                    request = """
                            {
                              "studentNumbers": ["2026000001", "2026000002"]
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "requested": 2,
                                "registered": 2,
                                "skipped": 0,
                                "tickets": [
                                  { "studentNumber": "2026000001", "ticketToken": "9f1c2e4a-7b8d-4c2a-9e3f-1a2b3c4d5e6f" },
                                  { "studentNumber": "2026000002", "ticketToken": "0a2d3f5b-8c9e-4d3b-af40-2b3c4d5e6f70" }
                                ]
                              }
                            }
                            """
            )
    )
    @PostMapping("/{date}/reservations")
    ResponseEntity<ApiResponse<ReserveResponse>> reserve(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody @Valid ReserveRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "야식 마차 목록 조회",
            description = "관리자가 전체 야식 마차 목록을 날짜 내림차순으로 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "nightSnackId", value = "야식 마차 ID입니다."),
                    @FieldDesc(name = "nightSnackDate", value = "야식 마차 날짜입니다."),
                    @FieldDesc(name = "status", value = "야식 마차 상태입니다. SCHEDULED / OPEN / CLOSED."),
                    @FieldDesc(name = "capacity", value = "전체 정원입니다."),
                    @FieldDesc(name = "remaining", value = "일반분 잔여 수량입니다."),
                    @FieldDesc(name = "reservedCapacity", value = "예약분 정원입니다."),
                    @FieldDesc(name = "reservedRemaining", value = "예약분 잔여 수량입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": [
                                {
                                  "nightSnackId": 2,
                                  "nightSnackDate": "2026-06-10",
                                  "status": "SCHEDULED",
                                  "capacity": 100,
                                  "remaining": 90,
                                  "reservedCapacity": 10,
                                  "reservedRemaining": 10,
                                  "openAt": "2026-06-10T17:30:00",
                                  "closeAt": "2026-06-10T18:30:00"
                                },
                                {
                                  "nightSnackId": 1,
                                  "nightSnackDate": "2026-06-03",
                                  "status": "CLOSED",
                                  "capacity": 100,
                                  "remaining": 0,
                                  "reservedCapacity": 10,
                                  "reservedRemaining": 8,
                                  "openAt": "2026-06-03T17:30:00",
                                  "closeAt": "2026-06-03T18:30:00"
                                }
                              ]
                            }
                            """
            )
    )
    @GetMapping
    ResponseEntity<ApiResponse<List<NightSnackResponse>>> listNightSnacks(
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "야식 마차 신청 성공 여부 조회",
            description = "관리자가 특정 야식 마차에 대해 학번으로 신청 성공 여부를 조회합니다. "
                    + "신청 내역이 없으면 applied: false를 반환하며, 예외를 던지지 않습니다. "
                    + "단, 서버 시간이 수령 마감 시각(pickupDeadline) 이상이면 신청 내역이 없어도 applied: true를 반환합니다.",
            descriptions = {
                    @FieldDesc(name = "nightSnackId", value = "조회할 야식 마차 ID입니다."),
                    @FieldDesc(name = "studentNumber", value = "조회할 학번입니다."),
                    @FieldDesc(name = "applied", value = "신청 성공 여부입니다. 신청 내역이 있거나 수령 마감 시각 이후이면 true."),
                    @FieldDesc(name = "source", value = "신청 구분입니다. GENERAL 또는 RESERVED. 신청 내역이 없으면 null."),
                    @FieldDesc(name = "status", value = "수령 상태입니다. 신청 내역이 없으면 null."),
                    @FieldDesc(name = "ticketToken", value = "QR 수령 티켓 값입니다. 신청 내역이 없으면 null.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "EVENT_NOT_FOUND", when = "존재하지 않는 야식 마차 ID로 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "applied": true,
                                "source": "GENERAL",
                                "status": "PENDING",
                                "ticketToken": "9f1c2e4a-7b8d-4c2a-9e3f-1a2b3c4d5e6f"
                              }
                            }
                            """
            )
    )
    @GetMapping("/{nightSnackId}/applications/{studentNumber}")
    ResponseEntity<ApiResponse<ApplicationCheckResponse>> checkApplication(
            @PathVariable Long nightSnackId,
            @PathVariable String studentNumber,
            @AuthenticatedMember MemberPrincipal principal
    );
}
