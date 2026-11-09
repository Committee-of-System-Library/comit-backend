package kr.ac.knu.comit.nightsnack;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knu.comit.ComitApplication;
import kr.ac.knu.comit.global.domain.Period;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnack;
import kr.ac.knu.comit.nightsnack.domain.NightSnackApplicationRepository;
import kr.ac.knu.comit.nightsnack.domain.NightSnackRepository;
import kr.ac.knu.comit.nightsnack.service.NightSnackInsertWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** INSERT Writer를 직접 호출한다. 기존 신청 서비스의 실행 경로는 전환하지 않는다. */
@Testcontainers
@SpringBootTest(classes = ComitApplication.class, properties = {
        "SPRING_PORT=0", "DDL_AUTO=validate", "MAX_FILE_SIZE=10MB", "MAX_REQUEST_SIZE=10MB",
        "S3_BUCKET_NAME=test-bucket", "S3_REGION=ap-northeast-2", "S3_ACCESS_KEY=test", "S3_SECRET_KEY=test",
        "OPENAI_API_KEY=ci-test-placeholder", "NOTICE_SCHEDULER_ENABLED=false",
        "comit.night-snack.scheduler-enabled=false",
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.vectorstore.qdrant.QdrantVectorStoreAutoConfiguration"
})
class NightSnackInsertWriterIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("comit_insert_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", MYSQL::getJdbcUrl);
        registry.add("DB_USERNAME", MYSQL::getUsername);
        registry.add("DB_PASSWORD", MYSQL::getPassword);
    }

    @MockitoBean VectorStore vectorStore;
    @Autowired NightSnackInsertWriter writer;
    @Autowired NightSnackRepository events;
    @Autowired NightSnackApplicationRepository applications;
    @Autowired MemberRepository members;

    @Test
    @DisplayName("정원 2명에 INSERT 접수 4건을 저장하고 같은 회원의 중복은 DB 제약으로 거부한다")
    void insertsBeyondCapacityAndRejectsDuplicates() {
        // given: 현재 기간 내 OPEN 행사와 네 회원을 준비한다.
        LocalDateTime now = LocalDateTime.now();
        NightSnack event = NightSnack.create(LocalDate.of(2037, 1, 1), 2, 0,
                Period.of(now.minusMinutes(1), now.plusHours(1)), null, null, null, null, null, null, false);
        event.open();
        event = events.save(event);
        Long eventId = event.getId();
        Member first = null;
        // when: 정원을 초과해도 신청 행을 INSERT한다.
        for (int i = 0; i < 4; i++) {
            Member member = members.save(Member.create("insert-test-" + i, "테스트", "010-0000-0000",
                    "insert-test-" + i, "202800000" + i, null, null, now));
            if (i == 0) first = member;
            assertThat(writer.reserve(member, eventId).getId()).isNotNull();
        }
        // then: 모든 접수 행은 남고, DB 유니크 제약이 중복을 막는다.
        assertThat(applications.countByNightSnackId(eventId)).isEqualTo(4);
        Member duplicate = first;
        assertThatThrownBy(() -> writer.reserve(duplicate, eventId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(applications.countByNightSnackId(eventId)).isEqualTo(4);
    }
}
