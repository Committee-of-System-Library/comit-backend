package kr.ac.knu.comit.nightsnack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.util.Optional;
import kr.ac.knu.comit.fixture.NightSnackFixture;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.domain.StudentCouncilFeeRepository;
import kr.ac.knu.comit.nightsnack.dto.NightSnackResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NightSnackQueryService")
class NightSnackQueryServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 8, 18);

    @Mock
    private NightSnackRepository nightSnackRepository;

    @Mock
    private NightSnackApplicationRepository nightSnackApplicationRepository;

    @Mock
    private StudentCouncilFeeRepository studentCouncilFeeRepository;

    @InjectMocks
    private NightSnackQueryService nightSnackQueryService;

    @Nested
    @DisplayName("getUpcomingFrom")
    class GetUpcomingFrom {

        @Test
        @DisplayName("기준일 당일을 포함해 가장 가까운 야식 마차를 반환한다")
        void returnsNearestUpcoming() {
            NightSnack nightSnack = NightSnackFixture.openNightSnack(11L, 100, 63);
            given(nightSnackRepository
                    .findFirstByNightSnackDateGreaterThanEqualOrderByNightSnackDateAsc(BASE_DATE))
                    .willReturn(Optional.of(nightSnack));

            NightSnackResponse response = nightSnackQueryService.getUpcomingFrom(BASE_DATE);

            assertThat(response.nightSnackId()).isEqualTo(11L);
            assertThat(response.remaining()).isEqualTo(63);
        }

        @Test
        @DisplayName("기준일을 그대로 조회 조건으로 넘긴다")
        void passesBaseDateToRepository() {
            given(nightSnackRepository
                    .findFirstByNightSnackDateGreaterThanEqualOrderByNightSnackDateAsc(BASE_DATE))
                    .willReturn(Optional.of(NightSnackFixture.openNightSnack(11L, 100, 63)));

            nightSnackQueryService.getUpcomingFrom(BASE_DATE);

            then(nightSnackRepository).should()
                    .findFirstByNightSnackDateGreaterThanEqualOrderByNightSnackDateAsc(BASE_DATE);
        }

        @Test
        @DisplayName("기준일 이후로 예정된 야식 마차가 없으면 EVENT_NOT_FOUND")
        void throwsWhenNoUpcoming() {
            given(nightSnackRepository
                    .findFirstByNightSnackDateGreaterThanEqualOrderByNightSnackDateAsc(BASE_DATE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> nightSnackQueryService.getUpcomingFrom(BASE_DATE))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(NightSnackErrorCode.EVENT_NOT_FOUND);
        }
    }
}
