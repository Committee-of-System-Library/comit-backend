package kr.ac.knu.comit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "SPRING_PORT=0",
        "MAX_FILE_SIZE=10MB",
        "MAX_REQUEST_SIZE=10MB",
        "DB_URL=jdbc:mysql://localhost:3306/test",
        "DB_USERNAME=test",
        "DB_PASSWORD=test",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class ComitApplicationTests {

    @Test
    void contextLoads() {
    }

}
