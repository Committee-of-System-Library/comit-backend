package kr.ac.knu.comit;

import kr.ac.knu.comit.comment.service.AdminCommentService;
import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.comment.service.CommentService;
import kr.ac.knu.comit.member.domain.MemberRepository;
import kr.ac.knu.comit.member.service.AdminMemberService;
import kr.ac.knu.comit.member.service.MemberRegistrationService;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.service.AdminPostService;
import kr.ac.knu.comit.post.service.PostService;
import kr.ac.knu.comit.report.service.AdminReportService;
import kr.ac.knu.comit.report.service.ReportService;
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
        "S3_BUCKET_NAME=test-bucket",
        "S3_REGION=ap-northeast-2",
        "S3_ACCESS_KEY=test",
        "S3_SECRET_KEY=test",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class ComitApplicationTests {

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    MemberRepository memberRepository;

    @MockitoBean
    MemberRegistrationService memberRegistrationService;

    @MockitoBean
    PostService postService;

    @MockitoBean
    CommentService commentService;

    @MockitoBean
    CommentQueryService commentQueryService;

    @MockitoBean
    ReportService reportService;

    @MockitoBean
    AdminReportService adminReportService;

    @MockitoBean
    AdminPostService adminPostService;

    @MockitoBean
    AdminCommentService adminCommentService;

    @MockitoBean
    AdminMemberService adminMemberService;

    @Test
    void contextLoads() {
    }

}
