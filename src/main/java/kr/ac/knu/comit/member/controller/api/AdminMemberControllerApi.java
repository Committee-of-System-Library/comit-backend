package kr.ac.knu.comit.member.controller.api;

import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.member.domain.MemberStatus;
import kr.ac.knu.comit.member.dto.AdminMemberPageResponse;
import kr.ac.knu.comit.member.dto.AdminMemberStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiContract
@RequestMapping("/admin/members")
public interface AdminMemberControllerApi {

    @ApiDoc(
            summary = "회원 목록 조회 (관리자)",
            description = "관리자가 회원 목록을 조회합니다. status로 필터링할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "status", value = "회원 상태 필터입니다. ACTIVE, SUSPENDED, BANNED 중 하나를 사용합니다. 생략하면 전체를 조회합니다."),
                    @FieldDesc(name = "members", value = "회원 요약 목록입니다."),
                    @FieldDesc(name = "page", value = "현재 페이지 번호입니다. 0부터 시작합니다."),
                    @FieldDesc(name = "size", value = "페이지 크기입니다."),
                    @FieldDesc(name = "totalElements", value = "전체 회원 수입니다."),
                    @FieldDesc(name = "totalPages", value = "전체 페이지 수입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "members": [
                                  {
                                    "id": 1,
                                    "nickname": "member-1",
                                    "studentNumber": "2021000001",
                                    "status": "ACTIVE",
                                    "suspendedUntil": null,
                                    "createdAt": "2026-03-28T10:00:00"
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
    ResponseEntity<ApiResponse<AdminMemberPageResponse>> getMembers(
            @RequestParam(required = false) MemberStatus status,
            Pageable pageable,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "회원 상태 변경 (관리자)",
            description = "관리자가 회원의 상태를 변경합니다. SUSPENDED 상태일 때 suspendedUntil을 설정할 수 있습니다.",
            descriptions = {
                    @FieldDesc(name = "memberId", value = "상태를 변경할 회원 ID입니다."),
                    @FieldDesc(name = "status", value = "변경할 상태입니다. ACTIVE, SUSPENDED, BANNED 중 하나를 사용합니다."),
                    @FieldDesc(name = "suspendedUntil", value = "정지 해제 시각입니다. SUSPENDED 상태일 때만 사용합니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "MEMBER_NOT_FOUND", when = "존재하지 않는 회원 ID로 요청할 때")
            },
            example = @Example(
                    request = """
                            {
                              "status": "SUSPENDED",
                              "suspendedUntil": "2026-04-28T10:00:00"
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PatchMapping("/{memberId}/status")
    ResponseEntity<ApiResponse<Void>> updateMemberStatus(
            @PathVariable Long memberId,
            @RequestBody @Valid AdminMemberStatusRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "회원 삭제 (관리자)",
            description = "관리자가 회원 계정을 소프트 삭제하고 개인정보를 비식별화합니다. 작성한 게시글·댓글은 유지되며 작성자 표시는 '탈퇴한 사용자'로 노출됩니다.",
            descriptions = {
                    @FieldDesc(name = "memberId", value = "삭제할 회원 ID입니다.")
            },
            errors = {
                    @ApiError(code = "FORBIDDEN", when = "관리자 권한이 없는 사용자가 요청할 때"),
                    @ApiError(code = "MEMBER_NOT_FOUND", when = "존재하지 않거나 이미 삭제된 회원 ID로 요청할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @DeleteMapping("/{memberId}")
    ResponseEntity<ApiResponse<Void>> deleteMember(
            @PathVariable Long memberId,
            @AuthenticatedMember MemberPrincipal principal
    );
}
