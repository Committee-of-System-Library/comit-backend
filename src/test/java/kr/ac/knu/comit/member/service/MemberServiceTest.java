package kr.ac.knu.comit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Optional;
import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.FakeMemberRepository;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.dto.UpdateNicknameRequest;
import kr.ac.knu.comit.member.dto.UpdateStudentNumberVisibilityRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MemberService")
class MemberServiceTest {

    private FakeMemberRepository memberRepository;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        memberService = new MemberService(memberRepository);
    }

    @Nested
    @DisplayName("findBySso")
    class FindBySso {

        @Test
        @DisplayName("기존 회원이 있으면 학번만 동기화해서 반환한다")
        void syncsStudentNumberWhenMemberAlreadyExists() {
            // given
            // 기존 활성 회원을 저장소에 등록한다.
            Member member = memberRepository.save(member("sso-1", "comit-user", "20230001"));
            MemberPrincipal principal = principal("sso-1", "comit-user", "20239999");

            // when
            Optional<Member> result = memberService.findBySso(principal);

            // then
            // 동일 회원이 반환되고 학번이 SSO 최신값으로 동기화된다.
            assertThat(result).containsSame(member);
            assertThat(result.orElseThrow().getStudentNumber()).isEqualTo("20239999");
        }

        @Test
        @DisplayName("기존 회원이 없으면 빈 Optional을 반환한다")
        void returnsEmptyWhenMemberDoesNotExist() {
            // given
            // 저장소가 비어 있다.
            MemberPrincipal principal = principal("sso-1", "comit-user", "20230001");

            // when
            Optional<Member> result = memberService.findBySso(principal);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("member existence")
    class MemberExistence {

        @Test
        @DisplayName("활성 회원 존재 여부를 조회한다")
        void returnsWhetherActiveMemberExists() {
            // given
            memberRepository.save(member("sso-1", "comit-user", "20230001"));

            // when
            boolean result = memberService.hasActiveMember("sso-1");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("삭제된 회원 포함 전체 존재 여부를 조회한다")
        void returnsWhetherAnyMemberExists() {
            // given
            memberRepository.save(member("sso-1", "comit-user", "20230001"));

            // when
            boolean result = memberService.hasAnyMember("sso-1");

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("updateNickname")
    class UpdateNickname {

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외를 던진다")
        void throwsWhenNicknameAlreadyExists() {
            // given
            // 대상 회원과 닉네임을 선점한 다른 회원을 저장한다.
            Member member = memberRepository.save(member("sso-1", "current", "20230001"));
            memberRepository.save(member("sso-2", "duplicate", "20230002"));

            // when & then
            assertThatThrownBy(() -> memberService.updateNickname(member.getId(), new UpdateNicknameRequest("duplicate")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        @Test
        @DisplayName("사용 가능한 닉네임이면 회원 닉네임을 수정한다")
        void updatesNicknameWhenNicknameIsAvailable() {
            // given
            Member member = memberRepository.save(member("sso-1", "old-name", "20230001"));

            // when
            memberService.updateNickname(member.getId(), new UpdateNicknameRequest("new-name"));

            // then
            assertThat(member.getNickname()).isEqualTo("new-name");
        }

        @Test
        @DisplayName("현재 닉네임과 같으면 중복 검사 없이 그대로 종료한다")
        void returnsWhenNicknameIsUnchanged() {
            // given
            Member member = memberRepository.save(member("sso-1", "same-name", "20230001"));

            // when
            memberService.updateNickname(member.getId(), new UpdateNicknameRequest("same-name"));

            // then
            // 닉네임이 변경되지 않고 저장소에 다른 회원이 없어도 예외가 없어야 한다.
            assertThat(member.getNickname()).isEqualTo("same-name");
        }
    }

    @Nested
    @DisplayName("updateStudentNumberVisibility")
    class UpdateStudentNumberVisibility {

        @Test
        @DisplayName("공개 여부 변경 요청이 오면 회원 상태를 갱신한다")
        void updatesStudentNumberVisibility() {
            // given
            Member member = memberRepository.save(member("sso-1", "comit-user", "20230001"));

            // when
            memberService.updateStudentNumberVisibility(member.getId(), new UpdateStudentNumberVisibilityRequest(false));

            // then
            assertThat(member.isStudentNumberVisible()).isFalse();
        }
    }

    @Nested
    @DisplayName("findMemberOrThrow")
    class FindMemberOrThrow {

        @Test
        @DisplayName("삭제된 회원이면 MEMBER_NOT_FOUND 예외를 던진다")
        void throwsWhenMemberIsDeleted() {
            // given
            // 저장 후 삭제 처리한 회원을 준비한다.
            Member member = memberRepository.save(member("sso-1", "comit-user", "20230001"));
            member.delete();

            // when & then
            assertThatThrownBy(() -> memberService.findMemberOrThrow(member.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

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
                null,
                LocalDateTime.parse("2026-03-31T12:00:00")
        );
    }
}
