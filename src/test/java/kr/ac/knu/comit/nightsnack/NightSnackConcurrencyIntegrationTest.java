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
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.service.AdminNightSnackService;
import kr.ac.knu.comit.nightsnack.service.NightSnackApplicationService;
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
 * Discussion #96 결정(모델 A + 방안 1 DB 원자적 UPDATE)을 실제 MySQL에서 경험적으로 검증하는 e2e 테스트.
 *
 * <p>핵심 질문: "수백 명이 동시에 신청해도 정원보다 많이 확정되지 않는가?" — 단위 테스트의 Mock으로는
 * 증명할 수 없고, 실제 DB의 원자적 UPDATE 직렬화가 있어야만 통과한다. 그래서 Testcontainers로
 * 진짜 MySQL 8.0을 띄운다.
 *
 * <p>주의: 클래스에 {@code @Transactional}을 붙이지 않는다. 각 신청은 자기 트랜잭션에서 커밋되어야
 * 동시성이 재현되기 때문이다(공유 트랜잭션이면 직렬화가 사라진다).
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
                "spring.autoconfigure.exclude="
                        + "org.springframework.ai.autoconfigure.vectorstore.qdrant.QdrantVectorStoreAutoConfiguration"
        }
)
@DisplayName("야식 마차 선착순 신청 동시성 (실제 MySQL)")
class NightSnackConcurrencyIntegrationTest {

    /** 테스트 간 공유 DB에서 회원 식별자 충돌을 막기 위한 전역 시퀀스. */
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
    private AdminNightSnackService adminNightSnackService;

    @Autowired
    private NightSnackRepository nightSnackRepository;

    @Autowired
    private NightSnackApplicationRepository nightSnackApplicationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("정원 100에 300명이 동시 신청해도 정확히 100명만 성공하고 잔여는 0이 된다")
    void onlyCapacitySucceedsUnderConcurrentApply() throws InterruptedException {
        // given
        // OPEN 상태의 정원 100 야식 마차와, 서로 다른 300명의 신청자를 준비한다.
        int capacity = 100;
        int contenders = 300;
        Long nightSnackId = openNightSnack(LocalDate.now().plusDays(1), capacity);
        List<Long> memberIds = seedMembers(contenders);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        // 풀 크기를 contenders와 동일하게 설정해야 한다.
        // newFixedThreadPool(32)이면 32개 스레드가 start.await()에서 블로킹되어
        // 나머지 268개가 ready.countDown()에 도달하지 못하고 deadlock이 발생한다.
        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(contenders);

        // when
        // 모든 스레드를 배리어에 모았다가 동시에 신청을 발사한다.
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
        // 타임아웃 시 불완전한 데이터로 assertion이 우연히 통과하는 것을 방지한다.
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // then
        // 초과 판매가 없어야 한다: 정확히 정원만큼만 성공, 나머지는 마감, 잔여 0, 저장된 신청도 정원과 일치.
        assertThat(success.get()).isEqualTo(capacity);
        assertThat(soldOut.get()).isEqualTo(contenders - capacity);
        assertThat(other.get()).isZero();
        assertThat(nightSnackRepository.findById(nightSnackId).orElseThrow().getRemaining()).isZero();
        // 테스트 간 DB를 공유하므로 전역 count가 아니라 이 야식 마차로 한정해 센다.
        assertThat(nightSnackApplicationRepository.countByNightSnackId(nightSnackId)).isEqualTo(capacity);
    }

    @Test
    @DisplayName("같은 회원이 동시에 여러 번 신청해도 단 1건만 성공한다")
    void sameMemberAppliesOnceUnderConcurrency() throws InterruptedException {
        // given
        // 정원이 충분한 야식 마차에 한 회원이 20번 동시 신청을 시도한다.
        int attempts = 20;
        Long nightSnackId = openNightSnack(LocalDate.now().plusDays(2), 100);
        Long memberId = seedMembers(1).get(0);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger alreadyApplied = new AtomicInteger();

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
                } catch (BusinessException exception) {
                    if (exception.getErrorCode() == NightSnackErrorCode.ALREADY_APPLIED) {
                        alreadyApplied.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // 카운트하지 않는다 — 아래 단언이 누락을 잡는다.
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // then
        // (member, nightSnack) 유니크 제약이 최종 방어선이므로 정확히 1건만 성공한다.
        assertThat(success.get()).isEqualTo(1);
        assertThat(alreadyApplied.get()).isEqualTo(attempts - 1);
        assertThat(nightSnackApplicationRepository.countByNightSnackId(nightSnackId)).isEqualTo(1);
    }

    private Long openNightSnack(LocalDate date, int capacity) {
        Period period = Period.of(date.atTime(17, 30), date.atTime(18, 30));
        NightSnack nightSnack = nightSnackRepository.save(
                NightSnack.create(date, capacity, 0, period, null, null, null, null, null, false));
        nightSnack.open();
        nightSnackRepository.save(nightSnack);
        return nightSnack.getId();
    }

    private List<Long> seedMembers(int count) {
        List<Long> ids = new ArrayList<>();
        for (int n = 0; n < count; n++) {
            // 테스트 간 MySQL 컨테이너를 공유하므로(클래스 단위 @Transactional 없음) 회원 식별자는
            // 전역 카운터로 유일하게 만든다. 그렇지 않으면 member 유니크 제약과 충돌한다.
            int i = MEMBER_SEQ.getAndIncrement();
            Member member = memberRepository.save(Member.create(
                    "sso-applicant-" + i,
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
        String candidate = "applicant" + i; // 닉네임 최대 15자
        return candidate.length() > 15 ? candidate.substring(0, 15) : candidate;
    }

    private String studentNumber(int i) {
        return String.format("2026%06d", i);
    }
}
