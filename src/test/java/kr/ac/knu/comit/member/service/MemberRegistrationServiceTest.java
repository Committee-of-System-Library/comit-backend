package kr.ac.knu.comit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @DisplayName("명시적으로 입력한 회원 정보로 가입시킨다")
    void registersMemberWithExplicitInput() {
        // given
        // 닉네임이 아직 사용 중이지 않은 회원가입 요청을 준비한다.
        given(memberRepository.existsByNickname("길동이")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        // 회원가입을 처리한다.
        Member member = memberRegistrationService.register(
                "sso-1",
                "홍길동",
                "010-1234-5678",
                "길동이",
                "2023012780",
                "심화",
                null
        );

        // then
        // 회원 정보와 동의 시각이 함께 저장되어야 한다.
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getPhone()).isEqualTo("010-1234-5678");
        assertThat(member.getNickname()).isEqualTo("길동이");
        assertThat(member.getStudentNumber()).isEqualTo("2023012780");
        assertThat(member.getMajorTrack()).isEqualTo("심화");
        assertThat(member.getAgreedAt()).isNotNull();
        then(memberRepository).should().existsByNickname("길동이");
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외를 던진다")
    void throwsWhenNicknameAlreadyExists() {
        // given
        // 같은 닉네임이 이미 선점된 상황을 준비한다.
        given(memberRepository.existsByNickname("길동이")).willReturn(true);

        // when & then
        // 저장 전에 중복 닉네임 예외가 반환되어야 한다.
        assertThatThrownBy(() -> memberRegistrationService.register(
                "sso-1",
                "홍길동",
                "010-1234-5678",
                "길동이",
                "2023012780",
                "심화",
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("저장 시 닉네임 unique 충돌이 나면 DUPLICATE_NICKNAME으로 변환한다")
    void throwsDuplicateNicknameWhenSaveCollidesOnNickname() {
        // given
        // 사전 조회는 통과했지만 DB 저장 시 닉네임 unique 충돌이 나는 상황을 준비한다.
        given(memberRepository.existsByNickname("길동이")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key for uk_member_nickname"));

        // when & then
        // 저장 충돌도 도메인 예외로 정규화되어야 한다.
        assertThatThrownBy(() -> memberRegistrationService.register(
                "sso-1",
                "홍길동",
                "010-1234-5678",
                "길동이",
                "2023012780",
                "심화",
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("저장 시 ssoSub unique 충돌이 나면 MEMBER_ALREADY_EXISTS로 변환한다")
    void throwsMemberAlreadyExistsWhenSaveCollidesOnSsoSub() {
        // given
        // 사전 조회 이후 다른 요청이 먼저 가입을 완료한 상황을 준비한다.
        given(memberRepository.existsByNickname("길동이")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key for uk_member_sso_sub"));

        // when & then
        // ssoSub 충돌은 이미 가입된 회원으로 해석되어야 한다.
        assertThatThrownBy(() -> memberRegistrationService.register(
                "sso-1",
                "홍길동",
                "010-1234-5678",
                "길동이",
                "2023012780",
                "심화",
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_ALREADY_EXISTS);
    }
}
