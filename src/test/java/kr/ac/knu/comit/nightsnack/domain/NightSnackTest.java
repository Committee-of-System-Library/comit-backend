package kr.ac.knu.comit.nightsnack.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("NightSnack")
class NightSnackTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("정원만큼 remaining을 채우고 SCHEDULED 상태로 생성한다")
        void createsScheduledNightSnackWithFullRemaining() {
            // when
            NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), 100);

            // then
            assertThat(nightSnack.getCapacity()).isEqualTo(100);
            assertThat(nightSnack.getRemaining()).isEqualTo(100);
            assertThat(nightSnack.getStatus()).isEqualTo(NightSnackStatus.SCHEDULED);
        }

        @Test
        @DisplayName("정원이 0 이하이면 INVALID_REQUEST가 발생한다")
        void throwsWhenCapacityNotPositive() {
            assertThatThrownBy(() -> NightSnack.create(LocalDate.of(2026, 5, 20), 0))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("날짜가 null이면 INVALID_REQUEST가 발생한다")
        void throwsWhenDateIsNull() {
            assertThatThrownBy(() -> NightSnack.create(null, 100))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("예약분을 지정하면 일반분 remaining과 예약분 reservedRemaining을 나눠 채운다")
        void splitsCapacityIntoGeneralAndReserved() {
            // when
            // 정원 100 중 10을 예약분으로 떼면 일반분은 90이다.
            NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), 100, 10);

            // then
            assertThat(nightSnack.getCapacity()).isEqualTo(100);
            assertThat(nightSnack.getReservedCapacity()).isEqualTo(10);
            assertThat(nightSnack.getReservedRemaining()).isEqualTo(10);
            assertThat(nightSnack.getRemaining()).isEqualTo(90);
            assertThat(nightSnack.generalCapacity()).isEqualTo(90);
        }

        @Test
        @DisplayName("예약분이 정원을 초과하면 INVALID_REQUEST가 발생한다")
        void throwsWhenReservedExceedsCapacity() {
            assertThatThrownBy(() -> NightSnack.create(LocalDate.of(2026, 5, 20), 100, 101))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("예약분이 음수면 INVALID_REQUEST가 발생한다")
        void throwsWhenReservedNegative() {
            assertThatThrownBy(() -> NightSnack.create(LocalDate.of(2026, 5, 20), 100, -1))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        @DisplayName("예약 잔여분 내에서 선점하면 reservedRemaining이 줄고 true를 반환한다")
        void decrementsReservedRemainingWithinCapacity() {
            // given
            NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), 100, 10);

            // when
            boolean reserved = nightSnack.reserve(3);

            // then
            assertThat(reserved).isTrue();
            assertThat(nightSnack.getReservedRemaining()).isEqualTo(7);
            // 일반분 remaining은 예약 선점의 영향을 받지 않는다.
            assertThat(nightSnack.getRemaining()).isEqualTo(90);
        }

        @Test
        @DisplayName("예약 잔여분을 넘기면 false를 반환하고 reservedRemaining을 건드리지 않는다")
        void returnsFalseWhenExceedingReservedRemaining() {
            // given
            NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), 100, 10);

            // when
            boolean reserved = nightSnack.reserve(11);

            // then
            assertThat(reserved).isFalse();
            assertThat(nightSnack.getReservedRemaining()).isEqualTo(10);
        }

        @Test
        @DisplayName("0 이하를 예약하면 false를 반환한다")
        void returnsFalseForNonPositiveCount() {
            NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), 100, 10);

            assertThat(nightSnack.reserve(0)).isFalse();
            assertThat(nightSnack.getReservedRemaining()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("open")
    class Open {

        @Test
        @DisplayName("SCHEDULED 상태에서 OPEN으로 전이한다")
        void transitionsFromScheduledToOpen() {
            // given
            NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), 100);

            // when
            nightSnack.open();

            // then
            assertThat(nightSnack.getStatus()).isEqualTo(NightSnackStatus.OPEN);
            assertThat(nightSnack.isOpen()).isTrue();
        }

        @Test
        @DisplayName("이미 OPEN인 야식 마차를 다시 오픈하면 INVALID_REQUEST가 발생한다")
        void throwsWhenAlreadyOpen() {
            // given
            NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), 100);
            ReflectionTestUtils.setField(nightSnack, "status", NightSnackStatus.OPEN);

            // when & then
            assertThatThrownBy(nightSnack::open)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("CLOSED 상태의 야식 마차는 오픈할 수 없다")
        void throwsWhenClosed() {
            // given
            NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), 100);
            ReflectionTestUtils.setField(nightSnack, "status", NightSnackStatus.CLOSED);

            // when & then
            assertThatThrownBy(nightSnack::open)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("sequenceFromRemaining")
    class SequenceFromRemaining {

        @Test
        @DisplayName("순번은 정원에서 잔여 수량을 뺀 값이다")
        void computesSequenceFromCapacityMinusRemaining() {
            // given
            NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), 100);
            ReflectionTestUtils.setField(nightSnack, "remaining", 63);

            // when & then
            // 100명 정원에서 63명 남았으면 37번째 신청자다.
            assertThat(nightSnack.sequenceFromRemaining()).isEqualTo(37);
        }
    }
}
