package kr.ac.knu.comit.notice.eval;

import java.util.List;

record NoticeRagEvalResult(
        String caseId,
        String question,
        String transformedQuery,
        List<Long> expectedNoticeIds,
        List<String> expectedTitleContains,
        List<Long> retrievedNoticeIds,
        List<String> retrievedTitles,
        List<Long> rerankedNoticeIds,
        List<Long> selectedNoticeIds,
        List<String> selectedTitles,
        List<Long> sourceNoticeIds,
        List<String> sourceTitles,
        String answer,
        EvalCheck retrieval,
        EvalCheck selection,
        EvalCheck source,
        EvalCheck mustContain,
        EvalCheck shouldNotContain
) {

    boolean isPass() {
        return retrieval.isPass()
                && selection.isPass()
                && source.isPass()
                && mustContain.isPass()
                && shouldNotContain.isPass();
    }

    String failureSummary() {
        return """
                caseId=%s
                question=%s
                transformedQuery=%s
                expected=%s
                retrieved=%s
                reranked=%s
                selected=%s
                sources=%s
                retrieval=%s
                selection=%s
                source=%s
                mustContain=%s
                shouldNotContain=%s
                answer=%s
                """.formatted(
                caseId,
                question,
                transformedQuery,
                expectedNoticeIds,
                expectedTitleContains,
                retrievedNoticeIds,
                retrievedTitles,
                rerankedNoticeIds,
                selectedNoticeIds,
                selectedTitles,
                sourceNoticeIds,
                sourceTitles,
                retrieval,
                selection,
                source,
                mustContain,
                shouldNotContain,
                answer
        );
    }

    record EvalCheck(
            boolean isPass,
            String reason
    ) {
    }
}
