package kr.ac.knu.comit.time.controller;

import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.time.controller.api.TimeControllerApi;
import kr.ac.knu.comit.time.dto.ServerTimeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TimeController implements TimeControllerApi {

    @Override
    public ResponseEntity<ApiResponse<ServerTimeResponse>> getServerTime() {
        return ResponseEntity.ok(ApiResponse.success(ServerTimeResponse.now()));
    }
}
