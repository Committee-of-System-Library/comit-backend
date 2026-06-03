package kr.ac.knu.comit.nightsnack.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.ac.knu.comit.fixture.NightSnackFixture;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NightSnackApplication")
class NightSnackApplicationTest {

    @Nested
    @DisplayName("general")
    class General {

        @Test
        @DisplayName("PENDING 상태와 티켓 토큰, 회원 학번을 가진 일반 신청을 생성한다")
        void createsPendingGeneralApplication() {
            // given
            Member member = MemberFixture.member(1L, "applicant");
            NightSnack nightSnack = NightSnackFixture.openNightSnack(10L, 100, 100);

            // when
            NightSnackApplication application = NightSnackApplication.general(member, nightSnack);

            // then
            assertThat(application.getStatus()).isEqualTo(NightSnackApplicationStatus.PENDING);
            assertThat(application.getSource()).isEqualTo(NightSnackApplicationSource.GENERAL);
            assertThat(application.getTicketToken()).isNotBlank();
            assertThat(application.getMember()).isEqualTo(member);
            assertThat(application.getNightSnack()).isEqualTo(nightSnack);
            assertThat(application.getStudentNumber()).isEqualTo(member.getStudentNumber());
            assertThat(application.getConfirmedAt()).isNull();
        }

        @Test
        @DisplayName("회원이 null이면 INVALID_REQUEST가 발생한다")
        void throwsWhenMemberIsNull() {
            NightSnack nightSnack = NightSnackFixture.openNightSnack(10L, 100, 100);

            assertThatThrownBy(() -> NightSnackApplication.general(null, nightSnack))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("reserved")
    class Reserved {

        @Test
        @DisplayName("회원 없이 학번만으로 사전신청을 생성한다")
        void createsReservedApplicationWithStudentNumberOnly() {
            // given
            NightSnack nightSnack = NightSnackFixture.reservedNightSnack(10L, 100, 10);

            // when
            NightSnackApplication application = NightSnackApplication.reserved(nightSnack, "2026000001");

            // then
            assertThat(application.getStatus()).isEqualTo(NightSnackApplicationStatus.PENDING);
            assertThat(application.getSource()).isEqualTo(NightSnackApplicationSource.RESERVED);
            assertThat(application.getMember()).isNull();
            assertThat(application.getStudentNumber()).isEqualTo("2026000001");
            assertThat(application.getTicketToken()).isNotBlank();
        }

        @Test
        @DisplayName("학번이 비어 있으면 INVALID_REQUEST가 발생한다")
        void throwsWhenStudentNumberIsBlank() {
            NightSnack nightSnack = NightSnackFixture.reservedNightSnack(10L, 100, 10);

            assertThatThrownBy(() -> NightSnackApplication.reserved(nightSnack, "  "))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("PENDING 상태에서 confirm하면 CONFIRMED로 전이하고 confirmedAt을 기록한다")
        void transitionsFromPendingToConfirmed() {
            // given
            NightSnackApplication application = NightSnackApplication.general(
                    MemberFixture.member(1L, "applicant"), NightSnackFixture.openNightSnack(10L, 100, 100));

            // when
            application.confirm();

            // then
            assertThat(application.getStatus()).isEqualTo(NightSnackApplicationStatus.CONFIRMED);
            assertThat(application.getConfirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 CONFIRMED된 신청을 다시 confirm하면 INVALID_REQUEST가 발생한다")
        void throwsWhenAlreadyConfirmed() {
            // given
            NightSnackApplication application = NightSnackApplication.general(
                    MemberFixture.member(1L, "applicant"), NightSnackFixture.openNightSnack(10L, 100, 100));
            application.confirm();

            // when & then
            assertThatThrownBy(application::confirm)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_REQUEST);
        }
    }
}
