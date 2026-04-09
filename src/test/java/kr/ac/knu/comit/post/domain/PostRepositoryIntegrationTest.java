package kr.ac.knu.comit.post.domain;

import jakarta.persistence.EntityManager;
import kr.ac.knu.comit.ComitApplication;
import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.comment.domain.CommentRepository;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Transactional
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
@DisplayName("PostRepository 인기글 집계")
class PostRepositoryIntegrationTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostDailyVisitorRepository postDailyVisitorRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("최근 7일 반응 점수로 상위 5개를 정렬하고 제외 대상을 걸러낸다")
    void findsTopFiveHotPostsByWeightedRecentSignals() {
        // given
        // 최근 반응, 오래된 반응, 삭제 상태가 섞인 게시글 데이터를 만든다.
        LocalDate today = LocalDate.now(KST);
        LocalDate startDate = today.minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime commonTieTime = startDateTime.plusHours(3);

        Member author = saveMember("author");
        Member member1 = saveMember("member1");
        Member member2 = saveMember("member2");
        Member member3 = saveMember("member3");
        Member member4 = saveMember("member4");

        Post topPost = savePost(author, "top-post", BoardType.QNA, startDateTime.plusDays(6));
        Post olderTenPost = savePost(author, "older-ten-post", BoardType.FREE, startDateTime.plusDays(2));
        Post newerTenPost = savePost(author, "newer-ten-post", BoardType.QNA, startDateTime.plusDays(5));
        Post lowerIdFourPost = savePost(author, "lower-id-four-post", BoardType.FREE, commonTieTime);
        Post higherIdFourPost = savePost(author, "higher-id-four-post", BoardType.QNA, commonTieTime);
        Post oldSignalOnlyPost = savePost(author, "old-signal-only-post", BoardType.QNA, startDateTime.plusDays(1));
        Post deletedPost = saveDeletedPost(author, "deleted-post", startDateTime.plusDays(4));

        saveLike(topPost, member1, startDateTime.plusHours(1));
        saveLike(topPost, member2, startDateTime.plusHours(2));
        saveComment(topPost, member3, startDateTime.plusHours(3), false);
        saveVisitor(topPost, member4, today);

        saveLike(olderTenPost, member1, startDateTime.plusHours(4));
        saveComment(olderTenPost, member2, startDateTime.plusHours(5), false);
        saveComment(olderTenPost, member3, startDateTime.plusHours(6), true);
        saveVisitor(olderTenPost, member4, today.minusDays(1));

        saveLike(newerTenPost, member1, startDateTime.plusHours(7));
        saveComment(newerTenPost, member2, startDateTime.plusHours(8), false);
        saveVisitor(newerTenPost, member3, today.minusDays(2));

        saveVisitor(lowerIdFourPost, member1, today);
        saveVisitor(lowerIdFourPost, member2, today.minusDays(1));

        saveVisitor(higherIdFourPost, member3, today);
        saveVisitor(higherIdFourPost, member4, today.minusDays(1));

        saveLike(oldSignalOnlyPost, member1, startDateTime.minusSeconds(1));
        saveComment(oldSignalOnlyPost, member2, startDateTime.minusSeconds(1), false);
        saveVisitor(oldSignalOnlyPost, member3, startDate.minusDays(1));

        saveLike(deletedPost, member1, startDateTime.plusHours(9));
        saveComment(deletedPost, member2, startDateTime.plusHours(10), false);
        saveVisitor(deletedPost, member3, today.minusDays(3));

        entityManager.flush();
        entityManager.clear();

        // when
        // 최근 7일 인기글 집계 쿼리를 실행한다.
        List<PostRepository.HotPostScoreView> results = postRepository.findHotPostScores(
                startDateTime, startDate, 5, 3, 2, false, List.of("NOTICE", "EVENT"), 5);

        // then
        // 가중치, 동점 정렬, 제외 규칙, 상위 5개 제한이 모두 반영되어야 한다.
        assertThat(results).hasSize(5);
        assertThat(results).extracting(PostRepository.HotPostScoreView::getPostId)
                .containsExactly(
                        topPost.getId(),
                        newerTenPost.getId(),
                        olderTenPost.getId(),
                        higherIdFourPost.getId(),
                        lowerIdFourPost.getId()
                );
        assertThat(results).extracting(PostRepository.HotPostScoreView::getScore)
                .containsExactly(15L, 10L, 10L, 4L, 4L);
    }

    @Test
    @DisplayName("최근 7일 unique 방문자는 동일 회원의 여러 날짜 조회를 1명으로 계산한다")
    void countsRecentVisitorsByDistinctMember() {
        // given
        // 같은 회원이 여러 날짜에 조회한 방문 데이터를 준비한다.
        LocalDate today = LocalDate.now(KST);
        LocalDate startDate = today.minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        Member author = saveMember("visitor-author");
        Member viewer = saveMember("repeated-viewer");

        Post post = savePost(author, "visitor-post", BoardType.QNA, startDateTime.plusDays(4));

        saveVisitor(post, viewer, today);
        saveVisitor(post, viewer, today.minusDays(2));

        entityManager.flush();
        entityManager.clear();

        // when
        // 최근 7일 인기글 집계 쿼리를 실행한다.
        List<PostRepository.HotPostScoreView> results = postRepository.findHotPostScores(
                startDateTime, startDate, 5, 3, 2, false, List.of("NOTICE", "EVENT"), 5);

        // then
        // 동일 회원의 여러 날짜 조회는 1명의 unique 방문자로 계산되어야 한다.
        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.getPostId()).isEqualTo(post.getId());
            assertThat(result.getScore()).isEqualTo(2L);
        });
    }

    private Member saveMember(String suffix) {
        String nickname = buildValidNickname(suffix);
        String studentNumber = buildStudentNumber(suffix);
        return memberRepository.save(Member.create(
                "sso-" + suffix,
                "테스트유저",
                "010-0000-0000",
                nickname,
                studentNumber,
                null,
                null,
                LocalDateTime.now()
        ));
    }

    private String buildValidNickname(String suffix) {
        String candidate = "n" + Integer.toUnsignedString(suffix.hashCode(), 36);
        return candidate.length() > 15 ? candidate.substring(0, 15) : candidate;
    }

    private String buildStudentNumber(String suffix) {
        return String.format("2023%06d", Math.floorMod(suffix.hashCode(), 1_000_000));
    }

    private Post savePost(Member author, String title, BoardType boardType, LocalDateTime createdAt) {
        Post post = Post.create(author, boardType, title, "content-" + title, List.of("spring"), List.of());
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        return postRepository.save(post);
    }

    private Post saveDeletedPost(Member author, String title, LocalDateTime createdAt) {
        Post post = Post.create(author, BoardType.QNA, title, "content-" + title, List.of(), List.of());
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        post.delete();
        return postRepository.save(post);
    }

    private void saveLike(Post post, Member member, LocalDateTime createdAt) {
        PostLike like = PostLike.of(post.getId(), member.getId());
        ReflectionTestUtils.setField(like, "createdAt", createdAt);
        postLikeRepository.save(like);
    }

    private void saveComment(Post post, Member author, LocalDateTime createdAt, boolean deleted) {
        Comment comment = Comment.create(post, author, "comment-" + post.getId() + "-" + createdAt);
        ReflectionTestUtils.setField(comment, "createdAt", createdAt);
        if (deleted) {
            ReflectionTestUtils.setField(comment, "deletedAt", createdAt.plusMinutes(1));
        }
        commentRepository.save(comment);
    }

    private void saveVisitor(Post post, Member member, LocalDate viewedOn) {
        postDailyVisitorRepository.save(PostDailyVisitor.of(post.getId(), member.getId(), viewedOn));
    }
}
