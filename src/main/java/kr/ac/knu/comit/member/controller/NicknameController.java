package kr.ac.knu.comit.member.controller;

import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.member.controller.api.NicknameControllerApi;
import kr.ac.knu.comit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NicknameController implements NicknameControllerApi {

    private final MemberService memberService;

    @Override
    public ResponseEntity<ApiResponse<Void>> checkNickname(String value) {
        memberService.checkNicknameAvailability(value);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
