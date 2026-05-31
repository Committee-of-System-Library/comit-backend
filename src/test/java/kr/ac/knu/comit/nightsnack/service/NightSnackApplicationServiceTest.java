package kr.ac.knu.comit.nightsnack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.sql.SQLException;
import java.util.Optional;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.fixture.NightSnackFixture;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.service.MemberService;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("NightSnackApplicationService")
class NightSnackApplicationServiceTest {

    @Mock
    private NightSnackRepository nightSnackRepository;

    @Mock
    private NightSnackApplicationRepository nightSnackApplicationRepository;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private NightSnackApplicationService nightSnackApplicationService;

    @Test
    @DisplayName("오픈된 야식 마차에 선점 성공하면 티켓 토큰과 순번을 반환한다")
    void returnsTicketAndSequenceWhenReservationSucceeds() {
        // given
        NightSnack beforeDecrement = NightSnackFixture.openNightSnack(10L, 100, 64);
        NightSnack afterDecrement = NightSnackFixture.openNightSnack(10L, 100, 63);
        given(nightSnackRepository.findById(10L))
                .willReturn(Optional.of(beforeDecrement), Optional.of(afterDecrement));
        given(nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(1L, 10L)).willReturn(false);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(1);
        given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L, "applicant"));
        given(nightSnackApplicationRepository.save(any(NightSnackApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ApplyResponse response = nightSnackApplicationService.apply(10L, 1L);

        // then
        assertThat(response.sequence()).isEqualTo(37);
        assertThat(response.remaining()).isEqualTo(63);
        assertThat(response.ticketToken()).isNotBlank();
        then(nightSnackApplicationRepository).should().save(any(NightSnackApplication.class));
    }

    @Test
    @DisplayName("존재하지 않는 야식 마차면 EVENT_NOT_FOUND를 반환한다")
    void throwsWhenNightSnackNotFound() {
        // given
        given(nightSnackRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> nightSnackApplicationService.apply(99L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.EVENT_NOT_FOUND);

        then(nightSnackRepository).should(never()).decrementRemaining(any());
    }

    @Test
    @DisplayName("이미 신청한 회원이면 선점 전에 ALREADY_APPLIED를 반환한다")
    void throwsWhenAlreadyAppliedBeforeDecrement() {
        // given
        given(nightSnackRepository.findById(10L)).willReturn(Optional.of(NightSnackFixture.openNightSnack(10L, 100, 100)));
        given(nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(1L, 10L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> nightSnackApplicationService.apply(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.ALREADY_APPLIED);

        then(nightSnackRepository).should(never()).decrementRemaining(any());
        then(nightSnackApplicationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("선점 실패 후 재조회 결과가 오픈 상태가 아니면 EVENT_NOT_OPEN을 반환한다")
    void throwsNightSnackNotOpenWhenReservationFailsAndNightSnackNotOpen() {
        // given
        given(nightSnackRepository.findById(10L))
                .willReturn(Optional.of(NightSnackFixture.scheduledNightSnack(10L, 100)),
                        Optional.of(NightSnackFixture.scheduledNightSnack(10L, 100)));
        given(nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(1L, 10L)).willReturn(false);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(0);

        // when & then
        assertThatThrownBy(() -> nightSnackApplicationService.apply(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.EVENT_NOT_OPEN);

        then(nightSnackApplicationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("선점 실패 후 재조회가 오픈 상태이면 EVENT_SOLD_OUT을 반환한다")
    void throwsSoldOutWhenReservationFailsButNightSnackOpen() {
        // given
        given(nightSnackRepository.findById(10L))
                .willReturn(Optional.of(NightSnackFixture.openNightSnack(10L, 100, 0)),
                        Optional.of(NightSnackFixture.openNightSnack(10L, 100, 0)));
        given(nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(1L, 10L)).willReturn(false);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(0);

        // when & then
        assertThatThrownBy(() -> nightSnackApplicationService.apply(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.EVENT_SOLD_OUT);
    }

    @Test
    @DisplayName("저장 단계의 유니크 제약 충돌도 ALREADY_APPLIED로 변환한다")
    void convertsDuplicateKeyViolationToAlreadyApplied() {
        // given
        given(nightSnackRepository.findById(10L)).willReturn(Optional.of(NightSnackFixture.openNightSnack(10L, 100, 64)));
        given(nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(1L, 10L)).willReturn(false);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(1);
        given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L, "applicant"));
        given(nightSnackApplicationRepository.save(any(NightSnackApplication.class))).willThrow(duplicateApplicationViolation());

        // when & then
        assertThatThrownBy(() -> nightSnackApplicationService.apply(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.ALREADY_APPLIED);
    }

    @Test
    @DisplayName("유니크 키가 아닌 무결성 오류는 그대로 전파한다")
    void propagatesNonDuplicateIntegrityViolations() {
        // given
        given(nightSnackRepository.findById(10L)).willReturn(Optional.of(NightSnackFixture.openNightSnack(10L, 100, 64)));
        given(nightSnackApplicationRepository.existsByMemberIdAndNightSnackId(1L, 10L)).willReturn(false);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(1);
        given(memberService.findMemberOrThrow(1L)).willReturn(MemberFixture.member(1L, "applicant"));
        given(nightSnackApplicationRepository.save(any(NightSnackApplication.class))).willThrow(foreignKeyViolation());

        // when & then
        assertThatThrownBy(() -> nightSnackApplicationService.apply(10L, 1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private DataIntegrityViolationException duplicateApplicationViolation() {
        SQLException sqlException = new SQLException(
                "Duplicate entry '1-10' for key 'uk_night_snack_application_member_night_snack'", "23000", 1062);
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "duplicate key", sqlException, "insert into night_snack_application ...", "uk_night_snack_application_member_night_snack");
        return new DataIntegrityViolationException("duplicate", constraintViolationException);
    }

    private DataIntegrityViolationException foreignKeyViolation() {
        SQLException sqlException = new SQLException(
                "Cannot add or update a child row: a foreign key constraint fails", "23000", 1452);
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "fk violation", sqlException, "insert into night_snack_application ...", "fk_application_member");
        return new DataIntegrityViolationException("fk", constraintViolationException);
    }
}
