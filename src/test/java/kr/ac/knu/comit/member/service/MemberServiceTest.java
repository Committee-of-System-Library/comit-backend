package kr.ac.knu.comit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.member.dto.UpdateNicknameRequest;
import kr.ac.knu.comit.member.dto.UpdateStudentNumberVisibilityRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberRegistrationService memberRegistrationService;

    @InjectMocks
    private MemberService memberService;

    @Nested
    @DisplayName("findOrCreateBySso")
    class FindOrCreateBySso {

        @Test
        @DisplayName("기존 회원이 있으면 학번만 동기화해서 반환한다")
        void syncsStudentNumberWhenMemberAlreadyExists() {
            Member member = Member.create("sso-1", "comit-user", "20230001");
            MemberPrincipal principal = principal("sso-1", "comit-user", "20239999");
            given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-1")).willReturn(Optional.of(member));

            Member result = memberService.findOrCreateBySso(principal);

            assertThat(result).isSameAs(member);
            assertThat(result.getStudentNumber()).isEqualTo("20239999");
            then(memberRepository).should().findBySsoSubAndDeletedAtIsNull("sso-1");
            then(memberRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("기존 회원이 없으면 새 회원을 저장한다")
        void createsMemberWhenMemberDoesNotExist() {
            MemberPrincipal principal = principal("sso-1", "comit-user", "20230001");
            Member createdMember = Member.create("sso-1", "comit-user", "20230001");
            given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-1")).willReturn(Optional.empty());
            given(memberRegistrationService.register(principal)).willReturn(createdMember);

            Member result = memberService.findOrCreateBySso(principal);

            assertThat(result).isSameAs(createdMember);
            assertThat(result.getSsoSub()).isEqualTo("sso-1");
            assertThat(result.getNickname()).isEqualTo("comit-user");
            assertThat(result.getStudentNumber()).isEqualTo("20230001");
            then(memberRegistrationService).should().register(principal);
        }

        @Test
        @DisplayName("동시 생성 충돌이 나면 이미 생성된 회원을 다시 조회해 반환한다")
        void reloadsMemberWhenConcurrentInsertCollides() {
            MemberPrincipal principal = principal("sso-1", "comit-user", "20239999");
            Member existingMember = Member.create("sso-1", "comit-user", "20230001");
            given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-1"))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.of(existingMember));
            given(memberRegistrationService.register(principal))
                    .willThrow(new DataIntegrityViolationException("duplicate"));

            Member result = memberService.findOrCreateBySso(principal);

            assertThat(result).isSameAs(existingMember);
            assertThat(result.getStudentNumber()).isEqualTo("20239999");
        }

        @Test
        @DisplayName("동시 생성 충돌 후 재조회에서도 없으면 원래 예외를 다시 던진다")
        void rethrowsWhenReloadFailsAfterConcurrentInsertCollision() {
            MemberPrincipal principal = principal("sso-1", "comit-user", "20239999");
            DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate");
            given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-1"))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.empty());
            given(memberRegistrationService.register(principal)).willThrow(exception);

            assertThatThrownBy(() -> memberService.findOrCreateBySso(principal))
                    .isSameAs(exception);
        }
    }

    @Nested
    @DisplayName("updateNickname")
    class UpdateNickname {

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외를 던진다")
        void throwsWhenNicknameAlreadyExists() {
            Member member = Member.create("sso-1", "current", "20230001");
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(memberRepository.existsByNicknameAndIdNot("duplicate", 1L)).willReturn(true);

            assertThatThrownBy(() -> memberService.updateNickname(1L, new UpdateNicknameRequest("duplicate")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemberErrorCode.DUPLICATE_NICKNAME);

            then(memberRepository).should().findById(1L);
            then(memberRepository).should().existsByNicknameAndIdNot("duplicate", 1L);
        }

        @Test
        @DisplayName("사용 가능한 닉네임이면 회원 닉네임을 수정한다")
        void updatesNicknameWhenNicknameIsAvailable() {
            Member member = Member.create("sso-1", "old-name", "20230001");
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(memberRepository.existsByNicknameAndIdNot("new-name", 1L)).willReturn(false);

            memberService.updateNickname(1L, new UpdateNicknameRequest("new-name"));

            assertThat(member.getNickname()).isEqualTo("new-name");
        }

        @Test
        @DisplayName("현재 닉네임과 같으면 중복 검사 없이 그대로 종료한다")
        void returnsWhenNicknameIsUnchanged() {
            Member member = Member.create("sso-1", "same-name", "20230001");
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            memberService.updateNickname(1L, new UpdateNicknameRequest("same-name"));

            assertThat(member.getNickname()).isEqualTo("same-name");
            then(memberRepository).should().findById(1L);
            then(memberRepository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("updateStudentNumberVisibility")
    class UpdateStudentNumberVisibility {

        @Test
        @DisplayName("공개 여부 변경 요청이 오면 회원 상태를 갱신한다")
        void updatesStudentNumberVisibility() {
            Member member = Member.create("sso-1", "comit-user", "20230001");
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            memberService.updateStudentNumberVisibility(1L, new UpdateStudentNumberVisibilityRequest(false));

            assertThat(member.isStudentNumberVisible()).isFalse();
        }
    }

    @Nested
    @DisplayName("findMemberOrThrow")
    class FindMemberOrThrow {

        @Test
        @DisplayName("삭제된 회원이면 MEMBER_NOT_FOUND 예외를 던진다")
        void throwsWhenMemberIsDeleted() {
            Member member = Member.create("sso-1", "comit-user", "20230001");
            member.delete();
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            assertThatThrownBy(() -> memberService.findMemberOrThrow(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private MemberPrincipal principal(String ssoSub, String name, String studentNumber) {
        return new MemberPrincipal(
                null,
                ssoSub,
                name,
                name + "@knu.ac.kr",
                studentNumber,
                MemberPrincipal.UserType.CSE_STUDENT,
                MemberPrincipal.MemberRole.STUDENT
        );
    }
}
