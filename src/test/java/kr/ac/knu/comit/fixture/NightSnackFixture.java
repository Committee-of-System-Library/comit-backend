package kr.ac.knu.comit.fixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.domain.Period;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnackStatus;
import org.springframework.test.util.ReflectionTestUtils;

public class NightSnackFixture {

    private static final LocalDate TEST_DATE = LocalDate.now().plusDays(7);

    /** 테스트용 기본 Period (17:30 오픈, 18:30 마감). */
    public static Period defaultPeriod() {
        LocalDateTime open = TEST_DATE.atTime(17, 30);
        return Period.of(open, open.plusHours(1));
    }

    /** SCHEDULED 상태의 야식 마차(예약분 없음). */
    public static NightSnack scheduledNightSnack(Long id, int capacity) {
        NightSnack nightSnack = NightSnack.create(TEST_DATE, capacity, 0, defaultPeriod(), null, null, null, null, null, null, false);
        ReflectionTestUtils.setField(nightSnack, "id", id);
        return nightSnack;
    }

    /** OPEN 상태의 야식 마차. */
    public static NightSnack openNightSnack(Long id, int capacity) {
        NightSnack nightSnack = scheduledNightSnack(id, capacity);
        ReflectionTestUtils.setField(nightSnack, "status", NightSnackStatus.OPEN);
        return nightSnack;
    }

    /** 즉시 확정 ID 구간을 가진 OPEN 행사. */
    public static NightSnack instantSelectionNightSnack(Long id, int capacity, long idBase) {
        NightSnack nightSnack = openNightSnack(id, capacity);
        ReflectionTestUtils.setField(nightSnack, "idBase", idBase);
        return nightSnack;
    }

    /** OPEN 상태의 학생회비 납부자 전용 야식 마차. */
    public static NightSnack openNightSnackRequiringStudentCouncilFee(Long id, int capacity) {
        NightSnack nightSnack = NightSnack.create(TEST_DATE, capacity, 0, defaultPeriod(), null, null, null, null, null, null, true);
        ReflectionTestUtils.setField(nightSnack, "id", id);
        ReflectionTestUtils.setField(nightSnack, "status", NightSnackStatus.OPEN);
        return nightSnack;
    }

    /** SCHEDULED 상태의 예약분이 있는 야식 마차. */
    public static NightSnack reservedNightSnack(Long id, int capacity, int reservedCapacity) {
        NightSnack nightSnack = NightSnack.create(TEST_DATE, capacity, reservedCapacity, defaultPeriod(), null, null, null, null, null, null, false);
        ReflectionTestUtils.setField(nightSnack, "id", id);
        return nightSnack;
    }

    /** 사전신청(RESERVED) 신청 건. */
    public static NightSnackApplication reservedApplication(Long id, NightSnack nightSnack, String studentNumber) {
        NightSnackApplication application = NightSnackApplication.reserved(nightSnack, studentNumber);
        ReflectionTestUtils.setField(application, "id", id);
        return application;
    }

    /** 일반 선착순(GENERAL) 신청 건. */
    public static NightSnackApplication generalApplication(Long id, NightSnack nightSnack, Member member) {
        NightSnackApplication application = NightSnackApplication.general(member, nightSnack);
        ReflectionTestUtils.setField(application, "id", id);
        return application;
    }
}
