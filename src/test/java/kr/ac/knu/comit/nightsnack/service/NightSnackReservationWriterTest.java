package kr.ac.knu.comit.nightsnack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.sql.SQLException;
import java.util.Optional;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.fixture.NightSnackFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("NightSnackReservationWriter (임계구역 — decrement→INSERT)")
class NightSnackReservationWriterTest {

    @Mock
    private NightSnackRepository nightSnackRepository;

    @Mock
    private NightSnackApplicationRepository nightSnackApplicationRepository;

    @InjectMocks
    private NightSnackReservationWriter reservationWriter;

    @Test
    @DisplayName("선점 성공하면 신청 엔티티(티켓 토큰 포함)를 저장하고 반환한다")
    void reservesAndSavesWhenDecrementSucceeds() {
        // given
        Member member = MemberFixture.member(1L, "applicant");
        NightSnack nightSnack = NightSnackFixture.openNightSnack(10L, 100, 64);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(1);
        given(nightSnackApplicationRepository.save(any(NightSnackApplication.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        NightSnackApplication application = reservationWriter.reserve(member, nightSnack);

        // then
        assertThat(application.getTicketToken()).isNotBlank();
        then(nightSnackApplicationRepository).should().save(any(NightSnackApplication.class));
    }

    @Test
    @DisplayName("선점 실패 후 재조회가 오픈 상태가 아니면 EVENT_NOT_OPEN이고 저장하지 않는다")
    void throwsNotOpenWhenDecrementFailsAndNotOpen() {
        // given
        Member member = MemberFixture.member(1L, "applicant");
        NightSnack nightSnack = NightSnackFixture.scheduledNightSnack(10L, 100);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(0);
        given(nightSnackRepository.findById(10L))
                .willReturn(Optional.of(NightSnackFixture.scheduledNightSnack(10L, 100)));

        // when & then
        assertThatThrownBy(() -> reservationWriter.reserve(member, nightSnack))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.EVENT_NOT_OPEN);

        then(nightSnackApplicationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("선점 실패 후 재조회가 오픈 상태이면 EVENT_SOLD_OUT")
    void throwsSoldOutWhenDecrementFailsButOpen() {
        // given
        Member member = MemberFixture.member(1L, "applicant");
        NightSnack nightSnack = NightSnackFixture.openNightSnack(10L, 100, 0);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(0);
        given(nightSnackRepository.findById(10L))
                .willReturn(Optional.of(NightSnackFixture.openNightSnack(10L, 100, 0)));

        // when & then
        assertThatThrownBy(() -> reservationWriter.reserve(member, nightSnack))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.EVENT_SOLD_OUT);
    }

    @Test
    @DisplayName("저장 단계의 유니크 제약 충돌은 ALREADY_APPLIED로 변환한다")
    void convertsDuplicateKeyViolationToAlreadyApplied() {
        // given
        Member member = MemberFixture.member(1L, "applicant");
        NightSnack nightSnack = NightSnackFixture.openNightSnack(10L, 100, 64);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(1);
        given(nightSnackApplicationRepository.save(any(NightSnackApplication.class)))
                .willThrow(duplicateApplicationViolation());

        // when & then
        assertThatThrownBy(() -> reservationWriter.reserve(member, nightSnack))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.ALREADY_APPLIED);
    }

    @Test
    @DisplayName("유니크 키가 아닌 무결성 오류는 그대로 전파한다")
    void propagatesNonDuplicateIntegrityViolations() {
        // given
        Member member = MemberFixture.member(1L, "applicant");
        NightSnack nightSnack = NightSnackFixture.openNightSnack(10L, 100, 64);
        given(nightSnackRepository.decrementRemaining(10L)).willReturn(1);
        given(nightSnackApplicationRepository.save(any(NightSnackApplication.class)))
                .willThrow(foreignKeyViolation());

        // when & then
        assertThatThrownBy(() -> reservationWriter.reserve(member, nightSnack))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private DataIntegrityViolationException duplicateApplicationViolation() {
        SQLException sqlException = new SQLException(
                "Duplicate entry '1-10' for key 'uk_night_snack_application_member_night_snack'", "23000", 1062);
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "duplicate key", sqlException, "insert into night_snack_application ...",
                "uk_night_snack_application_member_night_snack");
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
