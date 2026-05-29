package kr.ac.knu.comit.notice.scheduler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@RequiredArgsConstructor
@ConfigurationProperties(prefix = "comit.notice.scheduler")
@Getter
public class NoticeSchedulerProperties {

    private final int initialSyncMax;
    private final int latestSyncMaxPages;
    private final boolean reindexEmbeddingsOnStartup;
    private final int reindexEmbeddingsLimit;
}
