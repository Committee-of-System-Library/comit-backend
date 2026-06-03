package kr.ac.knu.comit.time;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.knu.comit.global.exception.GlobalExceptionHandler;
import kr.ac.knu.comit.time.controller.TimeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TimeController.class)
@Import(GlobalExceptionHandler.class)
class TimeControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 서버_시각을_인증_없이_반환한다() throws Exception {
        mockMvc.perform(get("/time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.serverTime").isNotEmpty())
                .andExpect(jsonPath("$.data.epoch").isNumber());
    }
}
