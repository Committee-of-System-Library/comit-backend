package kr.ac.knu.comit.notice.infrastructure.rag.config;

import java.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@RequiredArgsConstructor
@ConfigurationProperties(prefix = "comit.notice.rag")
@Getter
public class NoticeRagProperties {

    private final int retrievalTopK;
    private final double queryTransformThreshold;
    private final String queryTransformModel;
    private final String rerankModel;
    private final String answerNanoModel;
    private final String answerMiniModel;
    private final String answerModel;
    private final String summarizerModel;
    private final Duration chatOpenAiTimeout;
    private final int chatMaxConcurrency;
    private final int chatAcquireTimeoutSeconds;
    private final int chatResponseTimeoutSeconds;
}
