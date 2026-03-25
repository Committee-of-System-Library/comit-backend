package kr.ac.knu.comit;

import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.comment.service.CommentService;
import kr.ac.knu.comit.member.service.MemberRegistrationService;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    MemberRegistrationService memberRegistrationService;

    @MockitoBean
    PostService postService;

    @MockitoBean
    CommentService commentService;

    @MockitoBean
    CommentQueryService commentQueryService;

    @Test
    void contextLoads() {
    }

}
