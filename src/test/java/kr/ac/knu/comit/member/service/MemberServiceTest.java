package kr.ac.knu.comit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Nested
    @DisplayName("findBySso")
    class FindBySso {

        @Test
        @DisplayName("기존 회원이 있으면 학번만 동기화해서 반환한다")
        void syncsStudentNumberWhenMemberAlreadyExists() {
            Member member = member("sso-1", "comit-user", "20230001");
            MemberPrincipal principal = principal("sso-1", "comit-user", "20239999");
            given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-1")).willReturn(Optional.of(member));

            Optional<Member> result = memberService.findBySso(principal);

            assertThat(result).containsSame(member);
            assertThat(result.orElseThrow().getStudentNumber()).isEqualTo("20239999");
            then(memberRepository).should().findBySsoSubAndDeletedAtIsNull("sso-1");
            then(memberRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("기존 회원이 없으면 빈 Optional을 반환한다")
        void returnsEmptyWhenMemberDoesNotExist() {
            MemberPrincipal principal = principal("sso-1", "comit-user", "20230001");
            given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-1")).willReturn(Optional.empty());

            Optional<Member> result = memberService.findBySso(principal);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("member existence")
    class MemberExistence {

        @Test
        @DisplayName("활성 회원 존재 여부를 조회한다")
        void returnsWhetherActiveMemberExists() {
            given(memberRepository.findBySsoSubAndDeletedAtIsNull("sso-1")).willReturn(Optional.of(member("sso-1", "comit-user", "20230001")));

            boolean result = memberService.hasActiveMember("sso-1");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("삭제된 회원 포함 전체 존재 여부를 조회한다")
        void returnsWhetherAnyMemberExists() {
            given(memberRepository.existsBySsoSub("sso-1")).willReturn(true);

            boolean result = memberService.hasAnyMember("sso-1");

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("updateNickname")
    class UpdateNickname {

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외를 던진다")
        void throwsWhenNicknameAlreadyExists() {
            Member member = member("sso-1", "current", "20230001");
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
            Member member = member("sso-1", "old-name", "20230001");
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(memberRepository.existsByNicknameAndIdNot("new-name", 1L)).willReturn(false);

            memberService.updateNickname(1L, new UpdateNicknameRequest("new-name"));

            assertThat(member.getNickname()).isEqualTo("new-name");
        }

        @Test
        @DisplayName("현재 닉네임과 같으면 중복 검사 없이 그대로 종료한다")
        void returnsWhenNicknameIsUnchanged() {
            Member member = member("sso-1", "same-name", "20230001");
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
            Member member = member("sso-1", "comit-user", "20230001");
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
            Member member = member("sso-1", "comit-user", "20230001");
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

    private Member member(String ssoSub, String nickname, String studentNumber) {
        return Member.create(
                ssoSub,
                "테스트유저",
                "010-0000-0000",
                nickname,
                studentNumber,
                null,
                LocalDateTime.parse("2026-03-31T12:00:00")
        );
    }
}
