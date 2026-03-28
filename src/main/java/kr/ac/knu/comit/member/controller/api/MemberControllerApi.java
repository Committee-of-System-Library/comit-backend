package kr.ac.knu.comit.member.controller.api;

import jakarta.validation.Valid;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.member.dto.MemberProfileResponse;
import kr.ac.knu.comit.member.dto.UpdateNicknameRequest;
import kr.ac.knu.comit.member.dto.UpdateStudentNumberVisibilityRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/members/me")
public interface MemberControllerApi {

    @ApiDoc(
            summary = "내 프로필 조회",
            description = "현재 로그인한 회원의 프로필을 조회합니다.",
            descriptions = {
                    @FieldDesc(name = "id", value = "회원 고유 ID"),
                    @FieldDesc(name = "nickname", value = "현재 회원 닉네임"),
                    @FieldDesc(name = "studentNumber", value = "현재 회원의 학번"),
                    @FieldDesc(name = "studentNumberVisible", value = "학번 공개 여부")
            },
            errors = {
                    @ApiError(code = "MEMBER_NOT_FOUND", when = "인증된 사용자의 로컬 회원 정보가 존재하지 않을 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "id": 1,
                                "nickname": "comit-user",
                                "studentNumber": "20230001",
                                "studentNumberVisible": true
                              }
                            }
                            """
            )
    )
    @GetMapping
    ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "닉네임 수정",
            description = "현재 로그인한 회원의 닉네임을 수정합니다.",
            descriptions = {
                    @FieldDesc(name = "nickname", value = "1자 이상 15자 이하의 새 닉네임")
            },
            errors = {
                    @ApiError(code = "MEMBER_NOT_FOUND", when = "인증된 사용자의 로컬 회원 정보가 존재하지 않을 때"),
                    @ApiError(code = "DUPLICATE_NICKNAME", when = "이미 사용 중인 닉네임으로 변경하려고 할 때")
            },
            example = @Example(
                    request = """
                            {
                              "nickname": "backend-dev"
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PatchMapping
    ResponseEntity<ApiResponse<Void>> updateNickname(
            @RequestBody @Valid UpdateNicknameRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );

    @ApiDoc(
            summary = "학번 공개 여부 수정",
            description = "현재 로그인한 회원의 학번 공개 여부를 수정합니다.",
            descriptions = {
                    @FieldDesc(name = "visible", value = "true면 학번을 공개하고 false면 비공개합니다.")
            },
            errors = {
                    @ApiError(code = "MEMBER_NOT_FOUND", when = "인증된 사용자의 로컬 회원 정보가 존재하지 않을 때")
            },
            example = @Example(
                    request = """
                            {
                              "visible": false
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PatchMapping("/student-number-visibility")
    ResponseEntity<ApiResponse<Void>> updateStudentNumberVisibility(
            @RequestBody @Valid UpdateStudentNumberVisibilityRequest request,
            @AuthenticatedMember MemberPrincipal principal
    );
}
