package kr.ac.knu.comit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.List;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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

    @Test
    @DisplayName("저장 충돌이 나면 새 후보 닉네임으로 재시도한다")
    void retriesWithNewNicknameWhenSaveCollides() {
        // given
        // 첫 저장은 닉네임 충돌로 실패하고, 재시도에서는 suffix 닉네임이 비어 있는 상황을 준비한다.
        MemberPrincipal principal = principal("sso-1", "보형 장", "2023012780");
        given(memberRepository.existsByNickname("보형 장")).willReturn(false).willReturn(true);
        given(memberRepository.existsByNickname("보형 장-2780")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key for uk_member_nickname"))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        // 최초 가입을 실행한다.
        Member member = memberRegistrationService.register(principal);

        // then
        // 첫 시도 후 suffix 닉네임으로 재시도되어야 한다.
        assertThat(member.getNickname()).isEqualTo("보형 장-2780");

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        then(memberRepository).should(times(2)).existsByNickname("보형 장");
        then(memberRepository).should().existsByNickname("보형 장-2780");
        then(memberRepository).should(times(2)).saveAndFlush(memberCaptor.capture());
        List<Member> savedMembers = memberCaptor.getAllValues();
        assertThat(savedMembers).hasSize(2);
        assertThat(savedMembers.get(0).getNickname()).isEqualTo("보형 장");
        assertThat(savedMembers.get(1).getNickname()).isEqualTo("보형 장-2780");
    }

    @Test
    @DisplayName("닉네임 충돌이 계속되면 DUPLICATE_NICKNAME 예외를 반환한다")
    void throwsWhenNicknameCollisionPersists() {
        // given
        // 모든 재시도에서도 저장 충돌이 계속되는 상황을 준비한다.
        MemberPrincipal principal = principal("sso-1", "보형 장", "2023012780");
        given(memberRepository.existsByNickname("보형 장")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key for uk_member_nickname"));

        // when & then
        // bounded retry가 끝나면 도메인 예외로 종료되어야 한다.
        assertThatThrownBy(() -> memberRegistrationService.register(principal))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.DUPLICATE_NICKNAME);
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
