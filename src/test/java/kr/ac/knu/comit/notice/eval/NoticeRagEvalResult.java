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
        List<Double> retrievedScores,
        List<Long> rerankedNoticeIds,
        List<Long> selectedNoticeIds,
        List<String> selectedTitles,
        Integer appliedSourceLimit,
        List<Long> sourceNoticeIds,
        List<String> sourceTitles,
        String answer,
        Integer queryPromptTokens,
        Integer queryCompletionTokens,
        Integer queryTotalTokens,
        Integer rerankPromptTokens,
        Integer rerankCompletionTokens,
        Integer rerankTotalTokens,
        Integer answerPromptTokens,
        Integer answerCompletionTokens,
        Integer answerTotalTokens,
        EvalCheck retrieval,
        EvalCheck selection,
        EvalCheck source,
        EvalCheck mustContain,
        EvalCheck shouldNotContain
) {

    private static final int TOO_MANY_SOURCES_THRESHOLD = 5;
    private static final int HIGH_TOKEN_USAGE_THRESHOLD = 5_000;

    boolean isPass() {
        return retrieval.isPass()
                && selection.isPass()
                && source.isPass()
                && mustContain.isPass()
                && shouldNotContain.isPass();
    }

    EvalStatus status() {
        if (!isPass()) {
            return EvalStatus.FAIL;
        }
        if (!warnings().isEmpty()) {
            return EvalStatus.PASS_WITH_WARNING;
        }
        return EvalStatus.PASS;
    }

    List<String> warnings() {
        if (!isPass()) {
            return List.of();
        }

        List<String> warnings = new java.util.ArrayList<>();
        if (sourceNoticeIds.size() >= TOO_MANY_SOURCES_THRESHOLD) {
            warnings.add("TOO_MANY_SOURCES");
        }
        if (answerTotalTokens != null && answerTotalTokens >= HIGH_TOKEN_USAGE_THRESHOLD) {
            warnings.add("HIGH_TOKEN_USAGE");
        }
        return warnings;
    }

    String failReason() {
        if (status() == EvalStatus.PASS_WITH_WARNING) {
            return String.join(", ", warnings());
        }

        return List.of(
                        namedFailure("retrieval", retrieval),
                        namedFailure("selection", selection),
                        namedFailure("source", source),
                        namedFailure("mustContain", mustContain),
                        namedFailure("shouldNotContain", shouldNotContain)
                ).stream()
                .filter(reason -> !reason.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("PASS");
    }

    String failureSummary() {
        return """
                caseId=%s
                question=%s
                transformedQuery=%s
                expectedNoticeIds=%s
                expectedTitleContains=%s
                retrievedNoticeIds=%s
                retrievedTitles=%s
                reranked=%s
                selectedNoticeIds=%s
                selectedTitles=%s
                appliedSourceLimit=%s
                sourceNoticeIds=%s
                sourceTitles=%s
                queryTokens=%s/%s/%s
                rerankTokens=%s/%s/%s
                answerTokens=%s/%s/%s
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
                appliedSourceLimit,
                sourceNoticeIds,
                sourceTitles,
                queryPromptTokens,
                queryCompletionTokens,
                queryTotalTokens,
                rerankPromptTokens,
                rerankCompletionTokens,
                rerankTotalTokens,
                answerPromptTokens,
                answerCompletionTokens,
                answerTotalTokens,
                retrieval,
                selection,
                source,
                mustContain,
                shouldNotContain,
                answer
        );
    }

    private String namedFailure(String name, EvalCheck check) {
        if (check.isPass()) {
            return "";
        }
        return name + ":" + check.reason();
    }

    enum EvalStatus {
        PASS,
        PASS_WITH_WARNING,
        FAIL
    }

    record EvalCheck(
            boolean isPass,
            String reason
    ) {
    }
}
