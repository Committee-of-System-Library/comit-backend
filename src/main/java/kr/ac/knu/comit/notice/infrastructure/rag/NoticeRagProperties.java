package kr.ac.knu.comit.notice.infrastructure.rag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@RequiredArgsConstructor
@ConfigurationProperties(prefix = "comit.notice.rag")
@Getter
public class NoticeRagProperties {

    private final int retrievalTopK;
}
