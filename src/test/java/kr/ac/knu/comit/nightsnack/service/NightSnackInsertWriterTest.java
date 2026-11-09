package kr.ac.knu.comit.nightsnack.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import kr.ac.knu.comit.fixture.MemberFixture;
import kr.ac.knu.comit.global.domain.Period;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.nightsnack.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NightSnackInsertWriterTest {
    @Mock NightSnackRepository repository;
    @Mock NightSnackApplicationRepository applications;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T08:30:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("마감 시각부터는 스케줄러 실행 전에도 신청을 거부한다")
    void rejectsAtDeadline() {
        // given: 상태는 OPEN이지만 신청 기간은 종료되었다.
        var now = java.time.LocalDateTime.now(clock);
        NightSnack event = NightSnack.create(LocalDate.now(clock), 100, 0,
                Period.of(now.minusHours(1), now), null, null, null, null, null, null, false);
        event.open();
        given(repository.findForApplication(10L)).willReturn(Optional.of(event));
        var writer = new NightSnackInsertWriter(repository, applications, clock);
        // when / then: 시간 경계에서 새 접수는 허용하지 않는다.
        assertThatThrownBy(() -> writer.reserve(MemberFixture.member(1L, "tester"), 10L))
                .isInstanceOf(BusinessException.class).extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.EVENT_NOT_OPEN);
    }
}
