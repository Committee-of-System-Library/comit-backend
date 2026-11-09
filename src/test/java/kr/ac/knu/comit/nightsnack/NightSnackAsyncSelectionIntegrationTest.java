package kr.ac.knu.comit.nightsnack;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import kr.ac.knu.comit.ComitApplication;
import kr.ac.knu.comit.global.domain.Period;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NightSnackErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.nightsnack.domain.*;
import kr.ac.knu.comit.nightsnack.dto.ApplyResponse;
import kr.ac.knu.comit.nightsnack.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = ComitApplication.class, properties = {
        "SPRING_PORT=0", "DDL_AUTO=validate", "MAX_FILE_SIZE=10MB", "MAX_REQUEST_SIZE=10MB",
        "S3_BUCKET_NAME=test-bucket", "S3_REGION=ap-northeast-2", "S3_ACCESS_KEY=test", "S3_SECRET_KEY=test",
        "OPENAI_API_KEY=ci-test-placeholder", "NOTICE_SCHEDULER_ENABLED=false",
        "comit.night-snack.scheduler-enabled=false",
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.vectorstore.qdrant.QdrantVectorStoreAutoConfiguration"
})
@DisplayName("야식마차 INSERT 접수와 비동기 선정 (실제 MySQL)")
class NightSnackAsyncSelectionIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("comit_test").withUsername("test").withPassword("test")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", MYSQL::getJdbcUrl);
        registry.add("DB_USERNAME", MYSQL::getUsername);
        registry.add("DB_PASSWORD", MYSQL::getPassword);
    }

    @MockitoBean VectorStore vectorStore;
    @Autowired NightSnackApplicationService applications;
    @Autowired NightSnackInsertWriter writer;
    @Autowired NightSnackLifecycleService lifecycle;
    @Autowired AdminNightSnackService admin;
    @Autowired NightSnackQueryService queries;
    @Autowired NightSnackRepository events;
    @Autowired NightSnackApplicationRepository entries;
    @Autowired MemberRepository members;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Test
    @DisplayName("300명 동시 접수 후 스케줄러가 정확히 정원 100명만 확정하고 재실행해도 결과를 유지한다")
    void selectsCapacityAfterConcurrentInserts() throws Exception {
        // given: 자동 스케줄러를 끄고 300명이 정원 100명 행사에 동시에 접수한다.
        Long eventId = event(100, 0, true);
        List<Member> applicants = new ArrayList<>();
        for (int i = 0; i < 300; i++) applicants.add(member());
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ApplyResponse>> futures = new ArrayList<>();
        try {
            for (Member member : applicants) futures.add(pool.submit(() -> {
                await(start);
                return applications.apply(eventId, member.getId(), member.getStudentNumber());
            }));
            // when: 접수를 완료한 뒤 복수의 마감 작업을 실행한다.
            start.countDown();
            List<ApplyResponse> responses = new ArrayList<>();
            for (var future : futures) responses.add(future.get(60, TimeUnit.SECONDS));
            assertThat(responses).hasSize(300);
            assertThat(responses.stream().filter(r -> r.selectionStatus() == NightSnackSelectionStatus.ACCEPTED).count())
                    .isBetween(0L, 50L);
            assertThat(responses).filteredOn(r -> r.selectionStatus() == NightSnackSelectionStatus.PENDING)
                    .allSatisfy(r -> assertThat(r.ticketToken()).isNull());
            Future<?> first = pool.submit(() -> lifecycle.refresh(eventId));
            Future<?> second = pool.submit(() -> lifecycle.refresh(eventId));
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            // then: 접수 행은 유지되며 성공 100명, 탈락 200명이 된다. 즉시 성공은 뒤집히지 않는다.
            assertThat(count(eventId, "ACCEPTED")).isEqualTo(100);
            assertThat(count(eventId, "REJECTED")).isEqualTo(200);
            assertThat(count(eventId, "PENDING")).isZero();
            for (ApplyResponse response : responses) {
                if (response.selectionStatus() == NightSnackSelectionStatus.ACCEPTED)
                    assertThat(jdbc.queryForObject("SELECT selection_status FROM night_snack_application WHERE id=?",
                            String.class, response.applicationId())).isEqualTo("ACCEPTED");
            }
            assertThat(events.findById(eventId).orElseThrow().getStatus()).isEqualTo(NightSnackStatus.CLOSED);
            assertThatThrownBy(() -> applications.apply(eventId, member().getId(), null))
                    .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(NightSnackErrorCode.EVENT_NOT_OPEN);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("같은 회원의 동시 신청은 한 건만 접수하고 나머지는 중복 오류로 반환한다")
    void deduplicatesConcurrentApplications() throws Exception {
        // given: 동일 회원의 요청 10개를 동시에 보낸다.
        Long eventId = event(10, 0, true);
        Member member = member();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) futures.add(pool.submit(() -> {
                await(start);
                try {
                    applications.apply(eventId, member.getId(), member.getStudentNumber());
                    return true;
                } catch (BusinessException exception) {
                    assertThat(exception.getErrorCode()).isEqualTo(NightSnackErrorCode.ALREADY_APPLIED);
                    return false;
                }
            }));
            // when: 동시에 시작하고 모든 결과를 수집한다.
            start.countDown();
            int successes = 0;
            for (var future : futures) if (future.get(20, TimeUnit.SECONDS)) successes++;
            // then: 성공한 접수와 저장된 행은 각각 한 건이다.
            assertThat(successes).isEqualTo(1);
            assertThat(entries.countByNightSnackId(eventId)).isEqualTo(1);
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test
    @DisplayName("작은 ID의 커밋이 지연되면 마감이 기다리고 해당 신청을 우선 확정한다")
    void waitsForInFlightLowerIdBeforePublishing() throws Exception {
        // given: 첫 신청은 ID를 할당받고 커밋 직전에 대기한다. 두 번째 신청은 먼저 커밋된다.
        Long eventId = event(1, 0, true);
        Member slow = member();
        Member fast = member();
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch closing = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Long> slowId = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                Long id = writer.reserve(slow, eventId).getId();
                inserted.countDown();
                await(release);
                return id;
            }));
            assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue();
            ApplyResponse fastResponse = applications.apply(eventId, fast.getId(), fast.getStudentNumber());
            // when: 마감은 첫 신청의 공유 잠금이 해제될 때까지 완료되지 않는다.
            Future<?> close = pool.submit(() -> { closing.countDown(); lifecycle.refresh(eventId); });
            assertThat(closing.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> close.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            Long firstId = slowId.get(10, TimeUnit.SECONDS);
            close.get(10, TimeUnit.SECONDS);
            // then: 먼저 커밋한 두 번째 신청이 첫 신청을 밀어내지 않는다.
            assertThat(firstId).isLessThan(fastResponse.applicationId());
            assertThat(queries.getMyTicket(eventId, slow.getId(), "slow").selectionStatus()).isEqualTo(NightSnackSelectionStatus.ACCEPTED);
            assertThat(queries.getMyTicket(eventId, fast.getId(), "fast").selectionStatus()).isEqualTo(NightSnackSelectionStatus.REJECTED);
            assertThat(queries.getMyTicket(eventId, fast.getId(), "fast").ticketToken()).isNull();
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    @Test
    @DisplayName("롤백과 다른 행사로 ID가 비어도 실제 신청을 정렬해 정원을 채운다")
    void fillsCapacityDespiteIdGaps() {
        // given: 롤백으로 번호를 소비하고 다른 행사의 신청도 사이에 추가한다.
        Long eventId = event(4, 0, true);
        Long otherEvent = event(4, 0, true);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            writer.reserve(member(), eventId);
            status.setRollbackOnly();
        });
        Member other = member();
        applications.apply(otherEvent, other.getId(), other.getStudentNumber());
        // when: 원래 행사에는 정원만큼 접수한 뒤 마감한다.
        for (int i = 0; i < 4; i++) {
            Member member = member();
            applications.apply(eventId, member.getId(), member.getStudentNumber());
        }
        lifecycle.refresh(eventId);
        // then: ID 차이가 아니라 실제 4건이 모두 확정된다.
        assertThat(count(eventId, "ACCEPTED")).isEqualTo(4);
        assertThat(count(eventId, "REJECTED")).isZero();
    }

    @Test
    @DisplayName("예약분은 일반 정원 집계에서 제외하고 교차 중복 신청을 막는다")
    void keepsReservedCapacitySeparate() {
        // given: 일반 정원 3명과 예약분 2명 중 한 명을 사전 등록한다.
        Long eventId = event(5, 2, false);
        LocalDate date = events.findById(eventId).orElseThrow().getNightSnackDate();
        Member reserved = member();
        admin.reserve(date, List.of(reserved.getStudentNumber()));
        lifecycle.open(eventId);
        // when: 사전 등록 회원의 일반 신청은 거절하고 일반 신청 4건을 선정한다.
        assertThatThrownBy(() -> applications.apply(eventId, reserved.getId(), reserved.getStudentNumber()))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(NightSnackErrorCode.ALREADY_APPLIED);
        for (int i = 0; i < 4; i++) {
            Member member = member();
            applications.apply(eventId, member.getId(), member.getStudentNumber());
        }
        lifecycle.refresh(eventId);
        // then: 예약 1명과 일반 3명이 성공하며 남은 예약분은 유지한다.
        assertThat(count(eventId, "ACCEPTED")).isEqualTo(4);
        assertThat(count(eventId, "REJECTED")).isEqualTo(1);
        assertThat(events.findById(eventId).orElseThrow().getReservedRemaining()).isEqualTo(1);
    }

    @Test
    @DisplayName("기간 시작 시 자동 오픈하고 정원 미달이어도 시간 마감 시 모든 유효 신청을 확정한다")
    void opensAndClosesByTime() {
        // given: 신청 기간이 시작된 SCHEDULED 행사를 준비한다.
        Long eventId = event(10, 0, false);
        NightSnackScheduler scheduler = new NightSnackScheduler(events, lifecycle, Clock.systemDefaultZone());
        scheduler.refresh();
        assertThat(events.findById(eventId).orElseThrow().isOpen()).isTrue();
        Member member = member();
        applications.apply(eventId, member.getId(), member.getStudentNumber());
        // when: DB의 마감 시각을 과거로 바꿔 시간 만료를 재현한다.
        jdbc.update("UPDATE night_snack SET close_at=? WHERE id=?", LocalDateTime.now().minusSeconds(1), eventId);
        scheduler.refresh();
        // then: 정원 미달이어도 접수자를 확정하고 마감한다.
        assertThat(count(eventId, "ACCEPTED")).isEqualTo(1);
        assertThat(events.findById(eventId).orElseThrow().getStatus()).isEqualTo(NightSnackStatus.CLOSED);
    }

    @Test
    @DisplayName("일반 정원이 0이면 즉시 마감하고 일반 신청을 받지 않는다")
    void closesZeroGeneralCapacity() {
        // given: 모든 자리가 예약분인 행사다.
        Long eventId = event(1, 1, false);
        // when: 스케줄러가 행사 기간을 확인한다.
        lifecycle.refresh(eventId);
        // then: 일반 신청 없이 CLOSED가 된다.
        assertThat(events.findById(eventId).orElseThrow().getStatus()).isEqualTo(NightSnackStatus.CLOSED);
        assertThat(entries.countByNightSnackId(eventId)).isZero();
    }

    @Test
    @DisplayName("마감 상태 저장 실패 시 당락 일괄 갱신도 롤백되고 다음 실행에서 복구한다")
    void rollsBackSelectionTogetherWithClose() {
        // given: 두 신청 접수 후 CLOSED 변경만 DB에서 실패하도록 만든다.
        Long eventId = event(2, 0, true);
        for (int i = 0; i < 2; i++) {
            Member member = member();
            applications.apply(eventId, member.getId(), member.getStudentNumber());
        }
        jdbc.execute("CREATE TRIGGER fail_async_close BEFORE UPDATE ON night_snack FOR EACH ROW "
                + "BEGIN IF NEW.id = " + eventId + " AND NEW.status = 'CLOSED' THEN "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'test close failure'; END IF; END");
        try {
            // when / then: 최종 당락 UPDATE가 먼저 실행됐어도 마감 실패와 함께 롤백된다.
            assertThatThrownBy(() -> lifecycle.refresh(eventId)).isInstanceOf(RuntimeException.class);
            assertThat(count(eventId, "PENDING")).isEqualTo(2);
            assertThat(count(eventId, "ACCEPTED")).isZero();
            assertThat(events.findById(eventId).orElseThrow().isOpen()).isTrue();
        } finally {
            jdbc.execute("DROP TRIGGER fail_async_close");
        }
        lifecycle.refresh(eventId);
        assertThat(count(eventId, "ACCEPTED")).isEqualTo(2);
        assertThat(events.findById(eventId).orElseThrow().getStatus()).isEqualTo(NightSnackStatus.CLOSED);
    }

    private Long event(int capacity, int reserved, boolean open) {
        LocalDateTime now = LocalDateTime.now();
        NightSnack event = events.save(NightSnack.create(LocalDate.of(2030, 1, 1).plusDays(SEQ.incrementAndGet()),
                capacity, reserved, Period.of(now.minusMinutes(5), now.plusHours(1)),
                null, null, null, null, null, null, false));
        if (open) lifecycle.open(event.getId());
        return event.getId();
    }

    private Member member() {
        int n = SEQ.incrementAndGet();
        return members.save(Member.create("async-" + n, "신청자", "010-0000-0000", "async" + n,
                String.format("2028%06d", n), null, null, LocalDateTime.now()));
    }

    private int count(Long eventId, String status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM night_snack_application WHERE night_snack_id=? AND selection_status=?",
                Integer.class, eventId, status);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("latch timeout");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
