package kr.ac.knu.comit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.member.domain.MemberStatus;
import kr.ac.knu.comit.member.dto.AdminMemberStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminMemberService")
class AdminMemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AdminMemberService adminMemberService;

    @Test
    @DisplayName("미래 시각 정지 요청을 정상적으로 반영한다")
    void suspendsMemberUntilFutureTime() {
        // given
        // 미래 시각까지 정지할 회원을 준비한다.
        Member member = MemberFixture.member(1L, "member-1");
        LocalDateTime suspendedUntil = LocalDateTime.now().plusDays(1);
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));

        // when
        // 관리자 정지 요청을 처리한다.
        adminMemberService.updateMemberStatus(1L, new AdminMemberStatusRequest(MemberStatus.SUSPENDED, suspendedUntil));

        // then
        // 상태와 정지 시각이 모두 반영되어야 한다.
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(member.getSuspendedUntil()).isEqualTo(suspendedUntil);
        assertThat(member.isSuspended()).isTrue();
    }

    @Test
    @DisplayName("과거 시각 정지 요청은 INVALID_REQUEST로 거부한다")
    void throwsWhenSuspendedUntilIsInThePast() {
        // given
        // 이미 지난 시각까지 정지할 회원을 준비한다.
        Member member = MemberFixture.member(1L, "member-1");
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));

        // when & then
        // 과거 시각 정지는 도메인 규칙 위반으로 거부되어야 한다.
        assertThatThrownBy(() ->
                adminMemberService.updateMemberStatus(
                        1L,
                        new AdminMemberStatusRequest(MemberStatus.SUSPENDED, LocalDateTime.now().minusDays(1))
                ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }
}
