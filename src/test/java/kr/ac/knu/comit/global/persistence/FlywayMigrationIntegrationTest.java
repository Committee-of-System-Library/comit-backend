package kr.ac.knu.comit.global.persistence;

import kr.ac.knu.comit.ComitApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = ComitApplication.class,
        properties = {
                "SPRING_PORT=0",
                "DDL_AUTO=none",
                "MAX_FILE_SIZE=10MB",
                "MAX_REQUEST_SIZE=10MB",
                "COMIT_AUTH_BRIDGE_ENABLED=false"
        }
)
@DisplayName("Flyway 마이그레이션")
class FlywayMigrationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("comit_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", MYSQL::getJdbcUrl);
        registry.add("DB_USERNAME", MYSQL::getUsername);
        registry.add("DB_PASSWORD", MYSQL::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("애플리케이션 시작 시 Flyway 마이그레이션을 자동 적용한다")
    void appliesFlywayMigrationsOnStartup() {
        // given
        // 빈 MySQL 스키마로 애플리케이션 컨텍스트를 기동한다.

        // when
        // Flyway가 부팅 과정에서 기본 마이그레이션을 실행한다.
        Integer historyTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history'",
                Integer.class
        );
        Integer appliedMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class
        );
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() ORDER BY table_name",
                String.class
        );
        List<String> reportColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'report' ORDER BY ordinal_position",
                String.class
        );
        List<String> memberColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'member' ORDER BY ordinal_position",
                String.class
        );
        List<String> postColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'post' ORDER BY ordinal_position",
                String.class
        );
        List<String> commentColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'comment' ORDER BY ordinal_position",
                String.class
        );

        // then
        // Flyway 이력 테이블과 핵심 도메인 테이블이 모두 생성되어야 한다.
        assertThat(historyTableCount).isEqualTo(1);
        assertThat(appliedMigrationCount).isEqualTo(10);
        assertThat(tables).contains(
                "flyway_schema_history",
                "member",
                "post",
                "post_tag",
                "comment",
                "post_like",
                "comment_helpful",
                "post_daily_visitor",
                "report"
        );
        assertThat(reportColumns).contains("deleted_at");
        assertThat(memberColumns).contains("status", "suspended_until");
        assertThat(postColumns).contains("hidden_by_admin");
        assertThat(commentColumns).contains("hidden_by_admin");
    }
}
