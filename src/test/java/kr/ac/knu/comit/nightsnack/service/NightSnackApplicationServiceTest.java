package kr.ac.knu.comit.nightsnack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.fixture.NightSnackFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.domain.StudentCouncilFeeRepository;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NightSnackApplicationService (오케스트레이션 — 락 밖 읽기/조립)")
class NightSnackApplicationServiceTest {

    @Mock
    private NightSnackRepository nightSnackRepository;

    @Mock
    private NightSnackApplicationRepository nightSnackApplicationRepository;

    @Mock
    private StudentCouncilFeeRepository studentCouncilFeeRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private ReservationStrategy reservationStrategy;

    @InjectMocks
    private NightSnackApplicationService nightSnackApplicationService;

    @Test
    @DisplayName("선점 성공하면 티켓 토큰과 순번을 반환한다")
    void returnsTicketAndSequenceWhenReservationSucceeds() {
        // given
        NightSnack beforeDecrement = NightSnackFixture.openNightSnack(10L, 100, 64);
        NightSnack afterDecrement = NightSnackFixture.openNightSnack(10L, 100, 63);
        Member member = MemberFixture.member(1L, "applicant");
        NightSnackApplication application = NightSnackApplication.general(member, beforeDecrement);
        given(nightSnackRepository.findById(10L))
                .willReturn(Optional.of(beforeDecrement), Optional.of(afterDecrement));
        given(nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(1L, 10L)).willReturn(false);
        given(memberService.findMemberOrThrow(1L)).willReturn(member);
        given(reservationStrategy.reserve(any(Member.class), any(NightSnack.class))).willReturn(application);

        // when
        ApplyResponse response = nightSnackApplicationService.apply(10L, 1L, null);

        // then
        assertThat(response.sequence()).isEqualTo(37);
        assertThat(response.remaining()).isEqualTo(63);
        assertThat(response.ticketToken()).isNotBlank();
        then(reservationStrategy).should().reserve(any(Member.class), any(NightSnack.class));
    }

    @Test
    @DisplayName("존재하지 않는 야식 마차면 EVENT_NOT_FOUND이고 선점은 호출되지 않는다")
    void throwsWhenNightSnackNotFound() {
        // given
        given(nightSnackRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> nightSnackApplicationService.apply(99L, 1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.EVENT_NOT_FOUND);

        then(reservationStrategy).should(never()).reserve(any(), any());
    }

    @Test
    @DisplayName("이미 신청한 회원이면 선점 전에 ALREADY_APPLIED이고 회원 조회/선점은 호출되지 않는다")
    void throwsWhenAlreadyAppliedBeforeReserve() {
        // given
        given(nightSnackRepository.findById(10L))
                .willReturn(Optional.of(NightSnackFixture.openNightSnack(10L, 100, 100)));
        given(nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(1L, 10L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> nightSnackApplicationService.apply(10L, 1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.ALREADY_APPLIED);

        then(memberService).should(never()).findMemberOrThrow(any());
        then(reservationStrategy).should(never()).reserve(any(), any());
    }

    @Test
    @DisplayName("학생회비 납부자 전용 야식 마차는 is_paid=1인 학번만 신청할 수 있다")
    void appliesWhenStudentCouncilFeePaid() {
        // given
        NightSnack beforeDecrement = NightSnackFixture.openNightSnackRequiringStudentCouncilFee(10L, 100, 64);
        NightSnack afterDecrement = NightSnackFixture.openNightSnackRequiringStudentCouncilFee(10L, 100, 63);
        Member member = MemberFixture.member(1L, "applicant");
        NightSnackApplication application = NightSnackApplication.general(member, beforeDecrement);
        given(nightSnackRepository.findById(10L))
                .willReturn(Optional.of(beforeDecrement), Optional.of(afterDecrement));
        given(studentCouncilFeeRepository.existsPaidByStudentNumber("20230001")).willReturn(true);
        given(nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(1L, 10L)).willReturn(false);
        given(memberService.findMemberOrThrow(1L)).willReturn(member);
        given(reservationStrategy.reserve(any(Member.class), any(NightSnack.class))).willReturn(application);

        // when
        ApplyResponse response = nightSnackApplicationService.apply(10L, 1L, "20230001");

        // then
        assertThat(response.sequence()).isEqualTo(37);
        then(studentCouncilFeeRepository).should().existsPaidByStudentNumber("20230001");
        then(reservationStrategy).should().reserve(any(Member.class), any(NightSnack.class));
    }

    @Test
    @DisplayName("학생회비 납부자 전용 야식 마차는 is_paid=0이거나 납부 행이 없으면 신청할 수 없다")
    void throwsWhenStudentCouncilFeeNotPaid() {
        // given
        given(nightSnackRepository.findById(10L))
                .willReturn(Optional.of(NightSnackFixture.openNightSnackRequiringStudentCouncilFee(10L, 100, 100)));
        given(studentCouncilFeeRepository.existsPaidByStudentNumber("20230001")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> nightSnackApplicationService.apply(10L, 1L, "20230001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.STUDENT_COUNCIL_FEE_REQUIRED);

        then(nightSnackApplicationRepository).should(never()).existsByMemberIdAndNightSnackId(any(), any());
        then(memberService).should(never()).findMemberOrThrow(any());
        then(reservationStrategy).should(never()).reserve(any(), any());
    }
}
