package kr.ac.knu.comit.nightsnack;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.domain.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kr.ac.knu.comit.ComitApplication;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationStatus;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.service.NightSnackApplicationService;
import kr.ac.knu.comit.nightsnack.service.ReservationStrategy;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * {@code skip-locked} 전략(좌석 사전 생성 + {@code FOR UPDATE SKIP LOCKED})을 실제 MySQL에서 검증하는
 * e2e 테스트. {@link NightSnackConcurrencyIntegrationTest}(db-atomic-update)와 같은 시나리오·같은
 * 임계치를 쓰되, 이 전략은 {@code NightSnack.remaining} 을 갱신하지 않으므로 검증 방식이 다르다:
 * 좌석 행의 상태(AVAILABLE/PENDING) COUNT로 정합성을 확인한다.
 *
 * <p>클래스에 {@code @Transactional}을 붙이지 않는다 — 각 신청이 자기 트랜잭션에서 커밋되어야
 * 동시성이 재현된다.
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
                "S3_SECRET_KEY=test",
                "OPENAI_API_KEY=ci-test-placeholder",
                "NOTICE_SCHEDULER_ENABLED=false",
                "COMIT_NIGHT_SNACK_RESERVATION_STRATEGY=skip-locked",
                "spring.autoconfigure.exclude="
                        + "org.springframework.ai.autoconfigure.vectorstore.qdrant.QdrantVectorStoreAutoConfiguration"
        }
)
@DisplayName("야식 마차 선착순 신청 동시성 — skip-locked 전략 (실제 MySQL)")
class NightSnackSkipLockedConcurrencyIntegrationTest {

    private static final AtomicInteger MEMBER_SEQ = new AtomicInteger();

    @MockitoBean
    VectorStore vectorStore;

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
    private NightSnackApplicationService nightSnackApplicationService;

    @Autowired
    private ReservationStrategy reservationStrategy;

    @Autowired
    private NightSnackRepository nightSnackRepository;

    @Autowired
    private NightSnackApplicationRepository nightSnackApplicationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("정원 100에 300명이 동시 신청해도 정확히 100명만 성공하고 AVAILABLE 좌석은 0이 된다")
    void onlyCapacitySucceedsUnderConcurrentApply() throws InterruptedException {
        // given
        int capacity = 100;
        int contenders = 300;
        Long nightSnackId = openNightSnackWithSeats(LocalDate.now().plusDays(1), capacity);
        List<Long> memberIds = seedMembers(contenders);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(contenders);

        // when
        for (Long memberId : memberIds) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    nightSnackApplicationService.apply(nightSnackId, memberId, null);
                    success.incrementAndGet();
                } catch (BusinessException exception) {
                    if (exception.getErrorCode() == NightSnackErrorCode.EVENT_SOLD_OUT) {
                        soldOut.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                } catch (Exception exception) {
                    other.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // then
        // db-atomic-update 검증은 nightSnack.getRemaining()==0 을 보는데, skip-locked는 그 필드를
        // 건드리지 않으므로 좌석 상태 COUNT로 대신 검증한다.
        assertThat(success.get()).isEqualTo(capacity);
        assertThat(soldOut.get()).isEqualTo(contenders - capacity);
        assertThat(other.get()).isZero();
        assertThat(countByNightSnackAndStatus(nightSnackId, NightSnackApplicationStatus.AVAILABLE)).isZero();
        assertThat(countByNightSnackAndStatus(nightSnackId, NightSnackApplicationStatus.PENDING)).isEqualTo(capacity);
        assertThat(nightSnackApplicationRepository.countByNightSnackId(nightSnackId)).isEqualTo(capacity);
    }

    @Test
    @DisplayName("같은 회원이 동시에 여러 번 신청해도 단 1건만 성공하고 나머지 좌석은 AVAILABLE로 복구된다")
    void sameMemberAppliesOnceUnderConcurrency() throws InterruptedException {
        // given
        int attempts = 20;
        int capacity = 100;
        Long nightSnackId = openNightSnackWithSeats(LocalDate.now().plusDays(2), capacity);
        Long memberId = seedMembers(1).get(0);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(attempts);

        // when
        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    nightSnackApplicationService.apply(nightSnackId, memberId, null);
                    success.incrementAndGet();
                } catch (Exception exception) {
                    // atomic 계열과 달리 skip-locked는 서로 다른 좌석 행을 비직렬로 잡으므로, 20개 스레드가
                    // existsByMemberIdAndNightSnackId 사전 검사를 거의 동시에(둘 다 미커밋 상태로) 통과할 수
                    // 있다. 그 경우 나머지는 claimSeat UPDATE가 (member_id, night_snack_id) 유니크에 걸려
                    // BusinessException(ALREADY_APPLIED)이 아니라 DataIntegrityViolationException으로
                    // 실패한다 — 두 경로 다 "1건만 성공 + 좌석 자동 복구"라는 정합성은 동일하게 보장하므로
                    // 여기서는 에러 종류를 구분하지 않고 "성공 아님"으로만 센다.
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // then
        // 유니크 제약(member_id, night_snack_id) 위반으로 롤백된 좌석은 AVAILABLE로 자동 복구된다 —
        // 별도 보상 로직 없이도 좌석 수(capacity)가 그대로 보존돼야 한다.
        assertThat(success.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(attempts - 1);
        assertThat(nightSnackApplicationRepository.countByNightSnackId(nightSnackId)).isEqualTo(capacity);
        assertThat(countByNightSnackAndStatus(nightSnackId, NightSnackApplicationStatus.PENDING)).isEqualTo(1);
        assertThat(countByNightSnackAndStatus(nightSnackId, NightSnackApplicationStatus.AVAILABLE))
                .isEqualTo(capacity - 1);
    }

    private Long openNightSnackWithSeats(LocalDate date, int capacity) {
        LocalDateTime now = LocalDateTime.now();
        Period period = Period.of(now.minusMinutes(5), now.plusHours(1));
        NightSnack nightSnack = nightSnackRepository.save(
                NightSnack.create(date, capacity, 0, period, null, null, null, null, null, null, false));
        reservationStrategy.prepareSeats(nightSnack);
        nightSnack.open();
        nightSnackRepository.save(nightSnack);
        return nightSnack.getId();
    }

    private long countByNightSnackAndStatus(Long nightSnackId, NightSnackApplicationStatus status) {
        return nightSnackApplicationRepository.findAll().stream()
                .filter(a -> a.getNightSnack().getId().equals(nightSnackId) && a.getStatus() == status)
                .count();
    }

    private List<Long> seedMembers(int count) {
        List<Long> ids = new ArrayList<>();
        for (int n = 0; n < count; n++) {
            int i = MEMBER_SEQ.getAndIncrement();
            Member member = memberRepository.save(Member.create(
                    "sso-skiplocked-" + i,
                    "신청자",
                    "010-0000-0000",
                    nickname(i),
                    studentNumber(i),
                    null,
                    null,
                    LocalDateTime.now()
            ));
            ids.add(member.getId());
        }
        return ids;
    }

    private String nickname(int i) {
        String candidate = "skloc" + i; // 닉네임 최대 15자
        return candidate.length() > 15 ? candidate.substring(0, 15) : candidate;
    }

    private String studentNumber(int i) {
        return String.format("2027%06d", i);
    }
}
