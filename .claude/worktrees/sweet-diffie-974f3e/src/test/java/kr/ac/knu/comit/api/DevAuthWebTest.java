package kr.ac.knu.comit.api;

import static org.mockito.BDDMockito.given;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;
import kr.ac.knu.comit.auth.config.ComitDevAuthProperties;
import kr.ac.knu.comit.auth.config.ComitSsoProperties;
import kr.ac.knu.comit.auth.controller.DevAuthController;
import kr.ac.knu.comit.global.exception.GlobalExceptionHandler;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevAuthController.class)
@Import({
        GlobalExceptionHandler.class,
        ComitSsoProperties.class,
        ComitDevAuthProperties.class
})
@TestPropertySource(properties = {
        "comit.dev.auth.enabled=true",
        "comit.dev.auth.cookie-same-site=None",
        "comit.auth.sso.cookie-secure=true",
        "comit.auth.sso.cookie-path=/"
})
class DevAuthWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void issuesCrossSiteCompatibleCookieForDevAuthLogin() throws Exception {
        given(memberRepository.findByNicknameAndDeletedAtIsNull("관리자"))
                .willReturn(Optional.of(Member.create(
                        "dev-admin-001",
                        "관리자",
                        "010-1234-5678",
                        "관리자",
                        "2022000001",
                        null,
                        null,
                        LocalDateTime.now()
                )));

        mockMvc.perform(post("/auth/dev/login")
                        .param("nickname", "관리자")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("SameSite=None")))
                .andExpect(header().string("Set-Cookie", containsString("Secure")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }
}
