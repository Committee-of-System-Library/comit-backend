package kr.ac.knu.comit.notice.infrastructure.rag.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@RequiredArgsConstructor
@ConfigurationProperties(prefix = "comit.notice.rag")
@Getter
public class NoticeRagProperties {

    private final int retrievalTopK;
    private final String queryTransformModel;
    private final String rerankModel;
    private final String answerModel;
    private final String summarizerModel;
}
