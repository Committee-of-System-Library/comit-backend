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

        Path htmlFile = tempDir.resolve("payment/PaymentControllerApi.html");
        assertThat(Files.exists(htmlFile)).isTrue();
        assertThat(Files.exists(tempDir.resolve("index.html"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("index.js"))).isTrue();
        assertThat(Files.exists(staleFile)).isFalse();

        String html = Files.readString(htmlFile);
        assertThat(html).contains("결제 승인");
        assertThat(html).contains("결제 조회");
        assertThat(html).contains("주문 ID 기준으로 결제 상태를 조회합니다.");
        assertThat(html).contains("결제 승인 요청을 처리하고 승인 결과를 반환합니다.");
        assertThat(html).contains("경로 변수");
        assertThat(html).contains("쿼리 파라미터");
        assertThat(html).contains("includeHistory");
        assertThat(html).contains("/v1/payments/confirm");
        assertThat(html).contains("/v1/payments/{orderId}");
        assertThat(html).contains("paymentKey");
        assertThat(html).contains("type-chip type-string");
        assertThat(html).contains("type-chip type-number");
        assertThat(html).contains("type-chip type-datetime");

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
    }
}
