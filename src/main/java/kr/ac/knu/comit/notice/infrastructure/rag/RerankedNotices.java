package kr.ac.knu.comit.notice.infrastructure.rag;

import java.util.List;

public record RerankedNotices(
        List<Long> noticeIds,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
