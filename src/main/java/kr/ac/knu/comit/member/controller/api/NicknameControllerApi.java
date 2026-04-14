package kr.ac.knu.comit.member.controller.api;

import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiContract
@RequestMapping("/members/nicknames")
public interface NicknameControllerApi {

    @ApiDoc(
            summary = "닉네임 중복 검사",
            description = "닉네임이 이미 사용 중인지 확인합니다. 인증 없이 호출할 수 있으며 회원가입·프로필 수정 전에 사용합니다.",
            descriptions = {
                    @FieldDesc(name = "value", value = "중복 여부를 확인할 닉네임입니다.")
            },
            errors = {
                    @ApiError(code = "DUPLICATE_NICKNAME", when = "이미 사용 중인 닉네임일 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @GetMapping("/check")
    ResponseEntity<ApiResponse<Void>> checkNickname(@RequestParam String value);
}
