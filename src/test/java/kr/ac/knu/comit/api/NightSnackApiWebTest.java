package kr.ac.knu.comit.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import kr.ac.knu.comit.global.auth.MemberArgumentResolver;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.config.WebMvcConfig;
import kr.ac.knu.comit.global.exception.GlobalExceptionHandler;
import kr.ac.knu.comit.nightsnack.controller.NightSnackApplicationController;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationStatus;
import kr.ac.knu.comit.nightsnack.domain.NightSnackSelectionStatus;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.nightsnack.dto.MyTicketResponse;
import kr.ac.knu.comit.nightsnack.service.NightSnackApplicationService;
import kr.ac.knu.comit.nightsnack.service.NightSnackQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = NightSnackApplicationController.class, properties = "comit.auth.bridge.enabled=false")
@Import({WebMvcConfig.class, MemberArgumentResolver.class, GlobalExceptionHandler.class})
class NightSnackApiWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean NightSnackApplicationService applications;
    @MockitoBean NightSnackQueryService queries;
    private final MemberPrincipal principal = new MemberPrincipal(1L, "sso-1", "tester", null, "2028000001",
            MemberPrincipal.UserType.CSE_STUDENT, MemberPrincipal.MemberRole.STUDENT);

    @Test
    @DisplayName("접수 응답에 선정 대기 상태를 반환하고 순번·잔여와 QR 토큰을 제공하지 않는다")
    void returnsPendingApplicationContract() throws Exception {
        // given: 아직 최종 선정되지 않은 접수 결과다.
        given(applications.apply(10L, 1L, "2028000001"))
                .willReturn(new ApplyResponse(42L, NightSnackSelectionStatus.PENDING, null));
        // when / then: 당락 상태로 후속 조회를 안내한다.
        mvc.perform(post("/night-snacks/10/applications")
                        .requestAttr(MemberArgumentResolver.PRINCIPAL_ATTRIBUTE, principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(42))
                .andExpect(jsonPath("$.data.selectionStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.ticketToken").isEmpty())
                .andExpect(jsonPath("$.data.sequence").doesNotExist())
                .andExpect(jsonPath("$.data.remaining").doesNotExist());
    }

    @Test
    @DisplayName("내 티켓 조회는 선정 성공과 기존 수령 대기를 별도 필드로 반환한다")
    void returnsSelectionAndPickupSeparately() throws Exception {
        // given: 선정은 성공했고 아직 수령하지 않은 신청이다.
        LocalDateTime now = LocalDateTime.now();
        given(queries.getMyTicket(10L, 1L, "tester")).willReturn(new MyTicketResponse("tester", 42L, "ticket",
                NightSnackApplicationStatus.PENDING, NightSnackSelectionStatus.ACCEPTED,
                now.toLocalDate(), "menu", "location", now.plusHours(1), now, null));
        // when / then: 프론트가 당락과 수령 상태를 구분할 수 있다.
        mvc.perform(get("/night-snacks/10/my-ticket")
                        .requestAttr(MemberArgumentResolver.PRINCIPAL_ATTRIBUTE, principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectionStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.ticketToken").value("ticket"));
    }
}
