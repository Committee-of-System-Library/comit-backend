package kr.ac.knu.comit.notice.eval;

import java.util.List;

record NoticeRagEvalCase(
        String id,
        String question,
        String type,
        List<Long> expectedNoticeIds,
        List<String> expectedTitleContains,
        ExpectedNoticeMatch expectedMatch,
        EvalChecks checks,
        List<String> mustContain,
        List<String> shouldNotContain,
        String difficulty,
        String notes
) {

    enum ExpectedNoticeMatch {
        ANY,
        ALL
    }

    record EvalChecks(
            boolean retrieval,
            boolean selection,
            boolean source,
            boolean answer
    ) {
    }
}
