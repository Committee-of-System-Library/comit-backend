package kr.ac.knu.comit.nightsnack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knu.comit.global.domain.Period;
import java.util.concurrent.atomic.AtomicInteger;
import kr.ac.knu.comit.ComitApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationSource;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.dto.ReserveResponse;
import kr.ac.knu.comit.nightsnack.service.AdminNightSnackService;
import kr.ac.knu.comit.nightsnack.service.NightSnackApplicationService;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 요구사항 4-3(사전신청 예약분 + 일반 선착순)의 DB 레벨 보장을 실제 MySQL에서 검증한다.
 *
 * <p>Mock으로는 증명할 수 없는 것들을 본다:
 * <ul>
 *   <li>일반 선착순이 예약분을 침범하지 않고 일반분(capacity - reserved)에서만 마감되는가</li>
 *   <li>{@code (night_snack_id, student_number)} 유니크가 사전신청↔일반 교차 중복을 막는가</li>
 * </ul>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = ComitApplication.class,
        properties = {
                "SPRING_PORT=0",
                "DDL_AUTO=none",
                "MAX_FILE_SIZE=10MB",
                "MAX_REQUEST_SIZE=10MB",
                "S3_BUCKET_NAME=test-bucket",
                "S3_REGION=ap-northeast-2",
                "S3_ACCESS_KEY=test",
                "S3_SECRET_KEY=test"
        }
)
@DisplayName("야식 마차 사전신청/일반 선착순 분리 (실제 MySQL)")
class NightSnackReservationIntegrationTest {

    private static final AtomicInteger MEMBER_SEQ = new AtomicInteger();

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("comit_test")
            .withUsername("test")
            .withPassword("test")
            .withEnv("TZ", "Asia/Seoul");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", MYSQL::getJdbcUrl);
        registry.add("DB_USERNAME", MYSQL::getUsername);
        registry.add("DB_PASSWORD", MYSQL::getPassword);
    }

    @Autowired
    private AdminNightSnackService adminNightSnackService;

    @Autowired
    private NightSnackApplicationService nightSnackApplicationService;

    @Autowired
    private NightSnackRepository nightSnackRepository;

    @Autowired
    private NightSnackApplicationRepository nightSnackApplicationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("사전신청은 회원 없이 학번만으로 RESERVED 신청을 만들고 예약분만 차감한다")
    void preRegistrationConsumesReservedPoolOnly() {
        // given
        // 정원 20, 예약분 5 → 일반분 15.
        LocalDate date1 = LocalDate.now().plusDays(3);
        Period period1 = Period.of(date1.atTime(17, 30), date1.atTime(18, 30));
        Long nightSnackId = adminNightSnackService.createNightSnack(date1, 20, 5, period1);
        List<String> studentNumbers = List.of(uniqueStudentNumber(), uniqueStudentNumber());

        // when
        ReserveResponse response = adminNightSnackService.reserve(nightSnackId, studentNumbers);

        // then
        assertThat(response.registered()).isEqualTo(2);
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId).orElseThrow();
        assertThat(nightSnack.getReservedRemaining()).isEqualTo(3); // 5 - 2
        assertThat(nightSnack.getRemaining()).isEqualTo(15);        // 일반분은 그대로
        assertThat(nightSnackApplicationRepository.countByNightSnackId(nightSnackId)).isEqualTo(2);
        // 사전신청 행은 회원이 없고 source가 RESERVED다.
        assertThat(nightSnackApplicationRepository.findStudentNumbersByNightSnackId(nightSnackId))
                .containsExactlyInAnyOrderElementsOf(studentNumbers);
        nightSnackApplicationRepository.findAll().stream()
                .filter(application -> application.getNightSnack().getId().equals(nightSnackId))
                .forEach(application -> {
                    assertThat(application.getMember()).isNull();
                    assertThat(application.getSource()).isEqualTo(NightSnackApplicationSource.RESERVED);
                });
    }

    @Test
    @DisplayName("일반 선착순은 예약분을 침범하지 않고 일반분(capacity - reserved)에서만 마감된다")
    void generalApplyStopsAtGeneralCapacity() {
        // given
        // 정원 10, 예약분 3 → 일반분 7. 회원 10명이 일반 신청한다.
        int capacity = 10;
        int reserved = 3;
        LocalDate date2 = LocalDate.now().plusDays(4);
        Period period2 = Period.of(date2.atTime(17, 30), date2.atTime(18, 30));
        Long nightSnackId = adminNightSnackService.createNightSnack(date2, capacity, reserved, period2);
        adminNightSnackService.open(nightSnackId);

        int success = 0;
        int soldOut = 0;
        for (int i = 0; i < capacity; i++) {
            Long memberId = seedMember();
            try {
                nightSnackApplicationService.apply(nightSnackId, memberId);
                success++;
            } catch (BusinessException exception) {
                if (exception.getErrorCode() == NightSnackErrorCode.EVENT_SOLD_OUT) {
                    soldOut++;
                }
            }
        }

        // then
        assertThat(success).isEqualTo(capacity - reserved); // 7명만 성공
        assertThat(soldOut).isEqualTo(reserved);            // 나머지 3명은 마감
        NightSnack nightSnack = nightSnackRepository.findById(nightSnackId).orElseThrow();
        assertThat(nightSnack.getRemaining()).isZero();
        assertThat(nightSnack.getReservedRemaining()).isEqualTo(reserved); // 예약분은 손대지 않음
    }

    @Test
    @DisplayName("사전신청된 학번은 같은 마차의 일반 선착순으로 다시 신청할 수 없다(교차 중복)")
    void reservedStudentCannotApplyGenerally() {
        // given
        // 같은 학번을 가진 회원을 만들고, 그 학번을 먼저 사전신청해 둔다.
        String studentNumber = uniqueStudentNumber();
        LocalDate date3 = LocalDate.now().plusDays(5);
        Period period3 = Period.of(date3.atTime(17, 30), date3.atTime(18, 30));
        Long nightSnackId = adminNightSnackService.createNightSnack(date3, 10, 3, period3);
        adminNightSnackService.reserve(nightSnackId, List.of(studentNumber));
        adminNightSnackService.open(nightSnackId);

        Long memberId = seedMemberWithStudentNumber(studentNumber);
        int remainingBefore = nightSnackRepository.findById(nightSnackId).orElseThrow().getRemaining();

        // when & then
        // 일반 신청 시 (night_snack_id, student_number) 유니크에 걸려 ALREADY_APPLIED로 변환되고,
        // 롤백되어 일반분 잔여는 그대로 복구된다.
        assertThatThrownBy(() -> nightSnackApplicationService.apply(nightSnackId, memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(NightSnackErrorCode.ALREADY_APPLIED);

        int remainingAfter = nightSnackRepository.findById(nightSnackId).orElseThrow().getRemaining();
        assertThat(remainingAfter).isEqualTo(remainingBefore);
        // 사전신청 1건만 존재한다.
        assertThat(nightSnackApplicationRepository.countByNightSnackId(nightSnackId)).isEqualTo(1);
    }

    private Long seedMember() {
        return seedMemberWithStudentNumber(uniqueStudentNumber());
    }

    private Long seedMemberWithStudentNumber(String studentNumber) {
        int i = MEMBER_SEQ.getAndIncrement();
        Member member = memberRepository.save(Member.create(
                "sso-reserve-" + i,
                "신청자",
                "010-0000-0000",
                nickname(i),
                studentNumber,
                null,
                null,
                LocalDateTime.now()
        ));
        return member.getId();
    }

    private String nickname(int i) {
        String candidate = "reserve" + i;
        return candidate.length() > 15 ? candidate.substring(0, 15) : candidate;
    }

    private String uniqueStudentNumber() {
        return String.format("2027%06d", MEMBER_SEQ.getAndIncrement());
    }
}
