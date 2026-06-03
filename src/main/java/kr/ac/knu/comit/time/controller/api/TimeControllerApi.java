package kr.ac.knu.comit.time.controller.api;

import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.time.dto.ServerTimeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/time")
public interface TimeControllerApi {

    @ApiDoc(
            summary = "서버 현재 시각 조회",
            description = "클라이언트 로컬 시계 오차를 보정하기 위한 서버 기준 현재 시각을 반환합니다. "
                    + "선착순 신청·카운트다운 화면에서 epoch(밀리초)로 서버 시각과 동기화하는 데 사용하며, 인증이 필요 없습니다.",
            descriptions = {
                    @FieldDesc(name = "serverTime", value = "ISO-8601 오프셋 형식의 서버 시각 문자열입니다. 예: 2026-06-03T21:28:03.123+09:00"),
                    @FieldDesc(name = "epoch", value = "Unix epoch 기준 밀리초(ms) 정수입니다. JS의 Date.now()와 직접 비교할 수 있습니다.")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "serverTime": "2026-06-03T21:28:03.123+09:00",
                                "epoch": 1780489683123
                              }
                            }
                            """
            )
    )
    @GetMapping
    ResponseEntity<ApiResponse<ServerTimeResponse>> getServerTime();
}
