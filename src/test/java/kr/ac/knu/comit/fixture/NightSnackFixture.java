package kr.ac.knu.comit.fixture;

import java.time.LocalDate;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackStatus;
import org.springframework.test.util.ReflectionTestUtils;

public class NightSnackFixture {

    /** SCHEDULED 상태의 야식 마차(예약분 없음). */
    public static NightSnack scheduledNightSnack(Long id, int capacity) {
        NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), capacity);
        ReflectionTestUtils.setField(nightSnack, "id", id);
        return nightSnack;
    }

    /** SCHEDULED 상태의 예약분이 있는 야식 마차. */
    public static NightSnack reservedNightSnack(Long id, int capacity, int reservedCapacity) {
        NightSnack nightSnack = NightSnack.create(LocalDate.of(2026, 5, 20), capacity, reservedCapacity);
        ReflectionTestUtils.setField(nightSnack, "id", id);
        return nightSnack;
    }

    /** OPEN 상태의 야식 마차. remaining을 명시적으로 지정한다. */
    public static NightSnack openNightSnack(Long id, int capacity, int remaining) {
        NightSnack nightSnack = scheduledNightSnack(id, capacity);
        ReflectionTestUtils.setField(nightSnack, "status", NightSnackStatus.OPEN);
        ReflectionTestUtils.setField(nightSnack, "remaining", remaining);
        return nightSnack;
    }
}
