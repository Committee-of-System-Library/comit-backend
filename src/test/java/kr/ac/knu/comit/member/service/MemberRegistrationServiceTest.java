package kr.ac.knu.comit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberRegistrationService")
class MemberRegistrationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberRegistrationService memberRegistrationService;

    @Test
    @DisplayName("사용 가능한 표시 이름이면 그대로 초기 닉네임으로 저장한다")
    void registersWithPrincipalNameWhenNicknameIsAvailable() {
        // given
        // SSO 표시 이름이 비어 있지 않고 아직 사용 중이지 않은 상황을 준비한다.
        MemberPrincipal principal = principal("sso-1", "보형 장", "2023012780");
        given(memberRepository.existsByNickname("보형 장")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        // 최초 가입 요청을 처리한다.
        Member member = memberRegistrationService.register(principal);

        // then
        // 표시 이름이 그대로 초기 닉네임으로 저장된다.
        assertThat(member.getNickname()).isEqualTo("보형 장");
        assertThat(member.getStudentNumber()).isEqualTo("2023012780");
        then(memberRepository).should().existsByNickname("보형 장");
    }

    @Test
    @DisplayName("같은 표시 이름이 이미 있으면 학번 suffix를 붙여 저장한다")
    void registersWithStudentNumberSuffixWhenNicknameAlreadyExists() {
        // given
        // 표시 이름 충돌이 있지만 학번 suffix를 붙이면 비어 있는 상황을 준비한다.
        MemberPrincipal principal = principal("sso-1", "보형 장", "2023012780");
        given(memberRepository.existsByNickname("보형 장")).willReturn(true);
        given(memberRepository.existsByNickname("보형 장-2780")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        // 최초 가입 요청을 처리한다.
        Member member = memberRegistrationService.register(principal);

        // then
        // 닉네임 충돌을 피해 suffix가 붙은 초기 닉네임으로 저장된다.
        assertThat(member.getNickname()).isEqualTo("보형 장-2780");
        then(memberRepository).should().existsByNickname("보형 장");
        then(memberRepository).should().existsByNickname("보형 장-2780");
    }

    private MemberPrincipal principal(String ssoSub, String name, String studentNumber) {
        return new MemberPrincipal(
                null,
                ssoSub,
                name,
                "member@knu.ac.kr",
                studentNumber,
                MemberPrincipal.UserType.CSE_STUDENT,
                MemberPrincipal.MemberRole.STUDENT
        );
    }
}
