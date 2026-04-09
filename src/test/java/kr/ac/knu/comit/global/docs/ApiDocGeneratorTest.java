package kr.ac.knu.comit.global.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiDocGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesHtmlAndRemovesStaleFiles() throws Exception {
        Path staleFile = tempDir.resolve("stale/old.html");
        Files.createDirectories(staleFile.getParent());
        Files.writeString(staleFile, "stale");

        ApiDocGenerator.generate("kr.ac.knu.comit", tempDir);

        Path htmlFile = tempDir.resolve("member/MemberControllerApi.html");
        assertThat(Files.exists(htmlFile)).isTrue();
        assertThat(Files.exists(tempDir.resolve("auth/SsoAuthControllerApi.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("comment/AdminCommentControllerApi.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("comment/CommentControllerApi.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("member/AdminMemberControllerApi.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("member/MyMemberControllerApi.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("post/PostControllerApi.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("post/AdminPostControllerApi.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("report/AdminReportControllerApi.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("index.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("index.js"))).isTrue();
        assertThat(Files.exists(staleFile)).isFalse();

        String html = Files.readString(htmlFile);
        assertThat(html).contains("내 프로필 조회");
        assertThat(html).contains("닉네임 수정");
        assertThat(html).contains("학번 공개 여부 수정");
        assertThat(html).contains("/members/me");
        assertThat(html).contains("/members/me/student-number-visibility");
        assertThat(html).contains("type-chip type-string");
        assertThat(html).contains("type-chip type-boolean");

        String myMemberHtml = Files.readString(tempDir.resolve("member/MyMemberControllerApi.html"));
        assertThat(myMemberHtml).contains("내 활동 요약 조회");
        assertThat(myMemberHtml).contains("내가 쓴 게시글 목록 조회");
        assertThat(myMemberHtml).contains("내가 쓴 댓글 목록 조회");
        assertThat(myMemberHtml).contains("내가 좋아요한 게시글 목록 조회");
        assertThat(myMemberHtml).contains("/members/me/activity");
        assertThat(myMemberHtml).contains("/members/me/posts");
        assertThat(myMemberHtml).contains("/members/me/comments");
        assertThat(myMemberHtml).contains("/members/me/likes");

        String postHtml = Files.readString(tempDir.resolve("post/PostControllerApi.html"));
        assertThat(postHtml).contains("에러 응답 필드");
        assertThat(postHtml).contains("에러 코드");
        assertThat(postHtml).contains("에러 예시");
        assertThat(postHtml).contains("UNAUTHORIZED");
        assertThat(postHtml).contains("INVALID_REQUEST");
        assertThat(postHtml).contains("POST_NOT_FOUND");
        assertThat(postHtml).contains("/problems/common/unauthorized");
        assertThat(postHtml).contains("/problems/post/not-found");
        assertThat(postHtml).contains("errorCode");

        String authHtml = Files.readString(tempDir.resolve("auth/SsoAuthControllerApi.html"));
        assertThat(authHtml).contains("SSO 로그인 시작");
        assertThat(authHtml).contains("/auth/sso/login");
        assertThat(authHtml).contains("/auth/sso/callback");
    }
}
