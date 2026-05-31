package kr.ac.knu.comit.main.controller;

import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.main.controller.api.MainControllerApi;
import kr.ac.knu.comit.main.dto.MainPageResponse;
import kr.ac.knu.comit.main.service.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MainController implements MainControllerApi {

    private final MainService mainService;

    @Override
    public ResponseEntity<ApiResponse<MainPageResponse>> getMainPage() {
        return ResponseEntity.ok(ApiResponse.success(mainService.getMainPage()));
    }
}
