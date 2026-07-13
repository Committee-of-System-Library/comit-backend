package kr.ac.knu.comit.nightsnack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.fixture.NightSnackFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.nightsnack.config.NightSnackProperties;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationSource;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationStatus;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackStatus;
import kr.ac.knu.comit.nightsnack.dto.ApplicationCheckResponse;
import kr.ac.knu.comit.nightsnack.dto.NightSnackResponse;
import kr.ac.knu.comit.nightsnack.dto.ReserveResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminNightSnackService")
class AdminNightSnackServiceTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2026, 5, 20);
    private static final LocalDate NONEXISTENT_DATE = LocalDate.of(2099, 1, 1);

    @Mock
    private NightSnackRepository nightSnackRepository;

    @Mock
    private NightSnackApplicationRepository nightSnackApplicationRepository;

    @Mock
    private NightSnackProperties nightSnackProperties;

    @Mock
    private ReservationStrategy reservationStrategy;

    @Mock
    private Clock clock;

    @InjectMocks
    private AdminNightSnackService adminNightSnackService;

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("신규 학번을 예약분에 등록하고 티켓을 반환하며 예약 잔여분을 차감한다")
        void registersNewStudentNumbers() {
            // given
            NightSnack nightSnack = NightSnackFixture.reservedNightSnack(10L, 100, 10);
            given(nightSnackRepository.findByNightSnackDate(TEST_DATE)).willReturn(Optional.of(nightSnack));
            given(nightSnackApplicationRepository.findStudentNumbersByNightSnackId(10L)).willReturn(List.of());
            given(nightSnackApplicationRepository.save(any(NightSnackApplication.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ReserveResponse response = adminNightSnackService.reserve(TEST_DATE, List.of("2026000001", "2026000002"));

            // then
            assertThat(response.requested()).isEqualTo(2);
            assertThat(response.registered()).isEqualTo(2);
            assertThat(response.skipped()).isZero();
            assertThat(response.tickets()).hasSize(2);
            assertThat(response.tickets()).allSatisfy(ticket -> assertThat(ticket.ticketToken()).isNotBlank());
            assertThat(nightSnack.getReservedRemaining()).isEqualTo(8);
            then(nightSnackApplicationRepository).should(times(2)).save(any());
        }

        @Test
        @DisplayName("이미 등록된 학번은 건너뛰고 신규만 등록한다(멱등)")
        void skipsAlreadyRegistered() {
            // given
            NightSnack nightSnack = NightSnackFixture.reservedNightSnack(10L, 100, 10);
            given(nightSnackRepository.findByNightSnackDate(TEST_DATE)).willReturn(Optional.of(nightSnack));
            given(nightSnackApplicationRepository.findStudentNumbersByNightSnackId(10L))
                    .willReturn(List.of("2026000001"));
            given(nightSnackApplicationRepository.save(any(NightSnackApplication.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ReserveResponse response = adminNightSnackService.reserve(TEST_DATE, List.of("2026000001", "2026000002"));

            // then
            assertThat(response.requested()).isEqualTo(2);
            assertThat(response.registered()).isEqualTo(1);
            assertThat(response.skipped()).isEqualTo(1);
            assertThat(nightSnack.getReservedRemaining()).isEqualTo(9);
            then(nightSnackApplicationRepository).should(times(1)).save(any());
        }

        @Test
        @DisplayName("공백/중복 학번은 정규화되어 한 번만 집계된다")
        void normalizesWhitespaceAndDuplicates() {
            // given
            NightSnack nightSnack = NightSnackFixture.reservedNightSnack(10L, 100, 10);
            given(nightSnackRepository.findByNightSnackDate(TEST_DATE)).willReturn(Optional.of(nightSnack));
            given(nightSnackApplicationRepository.findStudentNumbersByNightSnackId(10L)).willReturn(List.of());
            given(nightSnackApplicationRepository.save(any(NightSnackApplication.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ReserveResponse response = adminNightSnackService.reserve(
                    TEST_DATE, List.of("2026000001", " 2026000001 ", "  ", "2026000002"));

            // then
            assertThat(response.requested()).isEqualTo(2);
            assertThat(response.registered()).isEqualTo(2);
            then(nightSnackApplicationRepository).should(times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("신규 학번 수가 예약 잔여분을 초과하면 RESERVED_CAPACITY_EXCEEDED를 던지고 저장하지 않는다")
        void throwsWhenExceedingReservedCapacity() {
            // given
            NightSnack nightSnack = NightSnackFixture.reservedNightSnack(10L, 100, 2);
            given(nightSnackRepository.findByNightSnackDate(TEST_DATE)).willReturn(Optional.of(nightSnack));
            given(nightSnackApplicationRepository.findStudentNumbersByNightSnackId(10L)).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> adminNightSnackService.reserve(
                    TEST_DATE, List.of("2026000001", "2026000002", "2026000003")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(NightSnackErrorCode.RESERVED_CAPACITY_EXCEEDED);

            then(nightSnackApplicationRepository).should(never()).save(any());
        }

        @ParameterizedTest(name = "{0} 상태면 RESERVATION_NOT_ALLOWED를 던진다")
        @EnumSource(value = NightSnackStatus.class, names = {"OPEN", "CLOSED"})
        @DisplayName("오픈 전(SCHEDULED)이 아니면 사전신청을 거부한다")
        void throwsWhenNotScheduled(NightSnackStatus status) {
            // given
            // OPEN 중 예약은 @DynamicUpdate 부재로 remaining 컬럼을 덮어써 oversell을 유발할 수 있어 막아야 한다.
            NightSnack nightSnack = NightSnackFixture.reservedNightSnack(10L, 100, 10);
            ReflectionTestUtils.setField(nightSnack, "status", status);
            given(nightSnackRepository.findByNightSnackDate(TEST_DATE)).willReturn(Optional.of(nightSnack));

            // when & then
            assertThatThrownBy(() -> adminNightSnackService.reserve(TEST_DATE, List.of("2026000001")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(NightSnackErrorCode.RESERVATION_NOT_ALLOWED);

            then(nightSnackApplicationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 날짜의 야식 마차면 EVENT_NOT_FOUND를 던진다")
        void throwsWhenNotFound() {
            // given
            given(nightSnackRepository.findByNightSnackDate(NONEXISTENT_DATE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminNightSnackService.reserve(NONEXISTENT_DATE, List.of("2026000001")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(NightSnackErrorCode.EVENT_NOT_FOUND);
        }

        @Test
        @DisplayName("유효한 학번이 하나도 없으면 INVALID_REQUEST를 던진다")
        void throwsWhenAllBlank() {
            // given
            NightSnack nightSnack = NightSnackFixture.reservedNightSnack(10L, 100, 10);
            given(nightSnackRepository.findByNightSnackDate(TEST_DATE)).willReturn(Optional.of(nightSnack));

            // when & then
            assertThatThrownBy(() -> adminNightSnackService.reserve(TEST_DATE, List.of("  ", "")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("listNightSnacks (야식 마차 목록 조회)")
    class ListNightSnacks {

        @Test
        @DisplayName("야식 마차 목록을 날짜 내림차순으로 반환한다")
        void returnsNightSnacksSortedByDateDesc() {
            // given
            NightSnack older = NightSnackFixture.scheduledNightSnack(1L, 100);
            NightSnack newer = NightSnackFixture.scheduledNightSnack(2L, 50);
            given(nightSnackRepository.findAllByOrderByNightSnackDateDesc())
                    .willReturn(List.of(newer, older));

            // when
            List<NightSnackResponse> response = adminNightSnackService.listNightSnacks();

            // then
            assertThat(response).hasSize(2);
            assertThat(response.get(0).nightSnackId()).isEqualTo(2L);
            assertThat(response.get(1).nightSnackId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("등록된 야식 마차가 없으면 빈 목록을 반환한다")
        void returnsEmptyListWhenNone() {
            // given
            given(nightSnackRepository.findAllByOrderByNightSnackDateDesc()).willReturn(List.of());

            // when
            List<NightSnackResponse> response = adminNightSnackService.listNightSnacks();

            // then
            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("checkApplication (신청 성공 여부 조회)")
    class CheckApplication {

        @Test
        @DisplayName("신청 내역이 있으면 applied: true와 상세 정보를 반환한다")
        void returnsTrueWhenApplied() {
            // given
            NightSnack nightSnack = NightSnackFixture.scheduledNightSnack(10L, 100);
            NightSnackApplication application = NightSnackFixture.reservedApplication(1L, nightSnack, "2026000001");
            given(nightSnackRepository.findById(10L)).willReturn(Optional.of(nightSnack));
            given(nightSnackApplicationRepository.findByNightSnackIdAndStudentNumber(10L, "2026000001"))
                    .willReturn(Optional.of(application));

            // when
            ApplicationCheckResponse response = adminNightSnackService.checkApplication(10L, "2026000001");

            // then
            assertThat(response.applied()).isTrue();
            assertThat(response.source()).isEqualTo(NightSnackApplicationSource.RESERVED);
            assertThat(response.status()).isEqualTo(NightSnackApplicationStatus.PENDING);
            assertThat(response.ticketToken()).isNotBlank();
        }

        @Test
        @DisplayName("신청 내역이 없고 수령 마감 전이면 applied: false를 반환한다")
        void returnsFalseWhenNotAppliedBeforePickupDeadline() {
            // given
            mockClockAt(LocalDateTime.of(2026, 5, 20, 18, 29, 59));
            NightSnack nightSnack = nightSnackWithPickupDeadline(LocalDateTime.of(2026, 5, 20, 18, 30));
            given(nightSnackRepository.findById(10L)).willReturn(Optional.of(nightSnack));
            given(nightSnackApplicationRepository.findByNightSnackIdAndStudentNumber(10L, "9999999999"))
                    .willReturn(Optional.empty());

            // when
            ApplicationCheckResponse response = adminNightSnackService.checkApplication(10L, "9999999999");

            // then
            assertThat(response.applied()).isFalse();
            assertThat(response.source()).isNull();
            assertThat(response.status()).isNull();
            assertThat(response.ticketToken()).isNull();
        }

        @Test
        @DisplayName("신청 내역이 없고 서버 시간이 수령 마감 시각과 같으면 applied: true와 null 상세 정보를 반환한다")
        void returnsTrueWhenNotAppliedAtPickupDeadline() {
            // given
            LocalDateTime pickupDeadline = LocalDateTime.of(2026, 5, 20, 18, 30);
            mockClockAt(pickupDeadline);
            NightSnack nightSnack = nightSnackWithPickupDeadline(pickupDeadline);
            given(nightSnackRepository.findById(10L)).willReturn(Optional.of(nightSnack));
            given(nightSnackApplicationRepository.findByNightSnackIdAndStudentNumber(10L, "9999999999"))
                    .willReturn(Optional.empty());

            // when
            ApplicationCheckResponse response = adminNightSnackService.checkApplication(10L, "9999999999");

            // then
            assertThat(response.applied()).isTrue();
            assertThat(response.source()).isNull();
            assertThat(response.status()).isNull();
            assertThat(response.ticketToken()).isNull();
        }

        @Test
        @DisplayName("신청 내역이 없고 서버 시간이 수령 마감 이후면 applied: true와 null 상세 정보를 반환한다")
        void returnsTrueWhenNotAppliedAfterPickupDeadline() {
            // given
            LocalDateTime pickupDeadline = LocalDateTime.of(2026, 5, 20, 18, 30);
            mockClockAt(pickupDeadline.plusSeconds(1));
            NightSnack nightSnack = nightSnackWithPickupDeadline(pickupDeadline);
            given(nightSnackRepository.findById(10L)).willReturn(Optional.of(nightSnack));
            given(nightSnackApplicationRepository.findByNightSnackIdAndStudentNumber(10L, "9999999999"))
                    .willReturn(Optional.empty());

            // when
            ApplicationCheckResponse response = adminNightSnackService.checkApplication(10L, "9999999999");

            // then
            assertThat(response.applied()).isTrue();
            assertThat(response.source()).isNull();
            assertThat(response.status()).isNull();
            assertThat(response.ticketToken()).isNull();
        }

        @Test
        @DisplayName("신청 내역이 없고 수령 마감 시각이 없으면 applied: false를 반환한다")
        void returnsFalseWhenNotAppliedAndPickupDeadlineIsNull() {
            // given
            NightSnack nightSnack = NightSnackFixture.scheduledNightSnack(10L, 100);
            given(nightSnackRepository.findById(10L)).willReturn(Optional.of(nightSnack));
            given(nightSnackApplicationRepository.findByNightSnackIdAndStudentNumber(10L, "9999999999"))
                    .willReturn(Optional.empty());

            // when
            ApplicationCheckResponse response = adminNightSnackService.checkApplication(10L, "9999999999");

            // then
            assertThat(response.applied()).isFalse();
            assertThat(response.source()).isNull();
            assertThat(response.status()).isNull();
            assertThat(response.ticketToken()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 야식 마차 ID면 EVENT_NOT_FOUND를 던진다")
        void throwsWhenNotFound() {
            // given
            given(nightSnackRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminNightSnackService.checkApplication(99L, "2026000001"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(NightSnackErrorCode.EVENT_NOT_FOUND);
        }

        private NightSnack nightSnackWithPickupDeadline(LocalDateTime pickupDeadline) {
            NightSnack nightSnack = NightSnackFixture.scheduledNightSnack(10L, 100);
            ReflectionTestUtils.setField(nightSnack, "pickupDeadline", pickupDeadline);
            return nightSnack;
        }

        private void mockClockAt(LocalDateTime dateTime) {
            ZoneId zone = ZoneId.of("Asia/Seoul");
            Instant instant = dateTime.atZone(zone).toInstant();
            given(clock.instant()).willReturn(instant);
            given(clock.getZone()).willReturn(zone);
        }
    }
}
