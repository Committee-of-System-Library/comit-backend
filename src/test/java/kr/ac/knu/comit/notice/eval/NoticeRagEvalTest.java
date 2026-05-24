package kr.ac.knu.comit.notice.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import kr.ac.knu.comit.notice.dto.NoticeSource;
import kr.ac.knu.comit.notice.eval.NoticeRagEvalCase.ExpectedNoticeMatch;
import kr.ac.knu.comit.notice.infrastructure.GeneratedAnswer;
import kr.ac.knu.comit.notice.infrastructure.NoticeAnswerGenerator;
import kr.ac.knu.comit.notice.infrastructure.NoticeDocumentSelector;
import kr.ac.knu.comit.notice.infrastructure.NoticeQueryTransformer;
import kr.ac.knu.comit.notice.infrastructure.NoticeQueryTypeClassifier;
import kr.ac.knu.comit.notice.infrastructure.NoticeReranker;
import kr.ac.knu.comit.notice.infrastructure.RerankedNotices;
import kr.ac.knu.comit.notice.infrastructure.TransformedQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "RUN_NOTICE_RAG_EVAL", matches = "true")
class NoticeRagEvalTest {

    private static final int EVAL_TOP_K = 20;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final NoticeRagEvalReportWriter REPORT_WRITER = new NoticeRagEvalReportWriter(OBJECT_MAPPER);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private NoticeQueryTransformer queryTransformer;

    @Autowired
    private NoticeReranker reranker;

    @Autowired
    private NoticeDocumentSelector documentSelector;

    @Autowired
    private NoticeQueryTypeClassifier queryTypeClassifier;

    @Autowired
    private NoticeAnswerGenerator answerGenerator;

    @Test
    void evaluateNoticeRagCases() throws Exception {
        List<NoticeRagEvalCase> cases = loadCases();

        List<NoticeRagEvalResult> results = cases.stream()
                .map(this::evaluate)
                .toList();

        REPORT_WRITER.write(results);

        assertThat(results)
                .filteredOn(result -> !result.isPass())
                .as(() -> buildFailureReport(results))
                .isEmpty();
    }

    private List<NoticeRagEvalCase> loadCases() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/notice-rag-eval-cases.json")) {
            return OBJECT_MAPPER.readValue(inputStream, new TypeReference<>() {
            });
        }
    }

    private NoticeRagEvalResult evaluate(NoticeRagEvalCase evalCase) {
        TransformedQuery transformedQuery = queryTransformer.transform(evalCase.question());
        List<Document> retrievedDocs = retrieve(transformedQuery.content());
        List<Long> retrievedNoticeIds = noticeIds(retrievedDocs);
        List<String> retrievedTitles = titles(retrievedDocs);

        RerankedNotices rerankedNotices = reranker.rerank(evalCase.question(), retrievedDocs);
        List<Document> selectedDocs = documentSelector.select(retrievedDocs, rerankedNotices.noticeIds());
        List<Long> selectedNoticeIds = noticeIds(selectedDocs);
        List<String> selectedTitles = titles(selectedDocs);
        int appliedSourceLimit = queryTypeClassifier.maxSources(evalCase.question());
        List<Document> answerDocs = selectedDocs.stream()
                .limit(appliedSourceLimit)
                .toList();

        GeneratedAnswer generatedAnswer = answerGenerator.generate(evalCase.question(), answerDocs);
        List<NoticeSource> sources = toSources(answerDocs);
        List<Long> sourceNoticeIds = sources.stream()
                .map(NoticeSource::noticeId)
                .toList();
        List<String> sourceTitles = sources.stream()
                .map(NoticeSource::title)
                .toList();

        return new NoticeRagEvalResult(
                evalCase.id(),
                evalCase.question(),
                transformedQuery.content(),
                evalCase.expectedNoticeIds(),
                evalCase.expectedTitleContains(),
                retrievedNoticeIds,
                retrievedTitles,
                rerankedNotices.noticeIds(),
                selectedNoticeIds,
                selectedTitles,
                appliedSourceLimit,
                sourceNoticeIds,
                sourceTitles,
                generatedAnswer.content(),
                transformedQuery.promptTokens(),
                transformedQuery.completionTokens(),
                transformedQuery.totalTokens(),
                rerankedNotices.promptTokens(),
                rerankedNotices.completionTokens(),
                rerankedNotices.totalTokens(),
                generatedAnswer.promptTokens(),
                generatedAnswer.completionTokens(),
                generatedAnswer.totalTokens(),
                evaluateExpected("retrieval", evalCase, retrievedNoticeIds, retrievedTitles),
                evaluateExpected("selection", evalCase, selectedNoticeIds, selectedTitles),
                evaluateExpected("source", evalCase, sourceNoticeIds, sourceTitles),
                evaluateMustContain(evalCase, generatedAnswer.content()),
                evaluateShouldNotContain(evalCase, generatedAnswer.content())
        );
    }

    private List<Document> retrieve(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(EVAL_TOP_K)
                        .build()
        );
    }

    private List<NoticeSource> toSources(List<Document> docs) {
        return docs.stream()
                .map(doc -> new NoticeSource(
                        parseNoticeId(doc),
                        metadataValue(doc, "title"),
                        metadataValue(doc, "originalUrl")
                ))
                .filter(source -> source.noticeId() != null)
                .toList();
    }

    private NoticeRagEvalResult.EvalCheck evaluateExpected(
            String step,
            NoticeRagEvalCase evalCase,
            List<Long> actualNoticeIds,
            List<String> actualTitles
    ) {
        if (!isCheckEnabled(evalCase, step)) {
            return new NoticeRagEvalResult.EvalCheck(true, step + " check disabled");
        }

        boolean noticeIdPass = evaluateNoticeIds(evalCase, actualNoticeIds);
        boolean titlePass = evaluateTitleContains(evalCase, actualTitles);

        return new NoticeRagEvalResult.EvalCheck(
                noticeIdPass && titlePass,
                "%s expectedNoticeIds=%s actualNoticeIds=%s expectedTitleContains=%s actualTitles=%s match=%s".formatted(
                        step,
                        evalCase.expectedNoticeIds(),
                        actualNoticeIds,
                        evalCase.expectedTitleContains(),
                        actualTitles,
                        evalCase.expectedMatch()
                )
        );
    }

    private NoticeRagEvalResult.EvalCheck evaluateMustContain(NoticeRagEvalCase evalCase, String answer) {
        if (!evalCase.checks().answer()) {
            return new NoticeRagEvalResult.EvalCheck(true, "answer check disabled");
        }

        List<String> missingKeywords = evalCase.mustContain().stream()
                .filter(keyword -> !contains(answer, keyword))
                .toList();

        return new NoticeRagEvalResult.EvalCheck(
                missingKeywords.isEmpty(),
                "missing=%s".formatted(missingKeywords)
        );
    }

    private NoticeRagEvalResult.EvalCheck evaluateShouldNotContain(NoticeRagEvalCase evalCase, String answer) {
        if (!evalCase.checks().answer()) {
            return new NoticeRagEvalResult.EvalCheck(true, "answer check disabled");
        }

        List<String> unexpectedKeywords = evalCase.shouldNotContain().stream()
                .filter(keyword -> contains(answer, keyword))
                .toList();

        return new NoticeRagEvalResult.EvalCheck(
                unexpectedKeywords.isEmpty(),
                "unexpected=%s".formatted(unexpectedKeywords)
        );
    }

    private boolean contains(String answer, String keyword) {
        return answer != null && keyword != null && answer.contains(keyword);
    }

    private List<Long> noticeIds(List<Document> docs) {
        return docs.stream()
                .map(this::parseNoticeId)
                .filter(id -> id != null)
                .toList();
    }

    private List<String> titles(List<Document> docs) {
        return docs.stream()
                .map(doc -> metadataValue(doc, "title"))
                .toList();
    }

    private boolean isCheckEnabled(NoticeRagEvalCase evalCase, String step) {
        return switch (step) {
            case "retrieval" -> evalCase.checks().retrieval();
            case "selection" -> evalCase.checks().selection();
            case "source" -> evalCase.checks().source();
            default -> true;
        };
    }

    private boolean evaluateNoticeIds(NoticeRagEvalCase evalCase, List<Long> actualNoticeIds) {
        List<Long> expectedNoticeIds = evalCase.expectedNoticeIds();
        if (expectedNoticeIds == null || expectedNoticeIds.isEmpty()) {
            return true;
        }

        return evalCase.expectedMatch() == ExpectedNoticeMatch.ALL
                ? actualNoticeIds.containsAll(expectedNoticeIds)
                : expectedNoticeIds.stream().anyMatch(actualNoticeIds::contains);
    }

    private boolean evaluateTitleContains(NoticeRagEvalCase evalCase, List<String> actualTitles) {
        List<String> expectedTitleContains = evalCase.expectedTitleContains();
        if (expectedTitleContains == null || expectedTitleContains.isEmpty()) {
            return true;
        }

        return evalCase.expectedMatch() == ExpectedNoticeMatch.ALL
                ? expectedTitleContains.stream().allMatch(keyword -> titleContains(actualTitles, keyword))
                : expectedTitleContains.stream().anyMatch(keyword -> titleContains(actualTitles, keyword));
    }

    private boolean titleContains(List<String> actualTitles, String keyword) {
        return actualTitles.stream()
                .anyMatch(title -> contains(title, keyword));
    }

    private Long parseNoticeId(Document doc) {
        Object value = doc.getMetadata().get("noticeId");
        if (value == null) {
            return null;
        }
        return Long.parseLong(value.toString());
    }

    private String metadataValue(Document doc, String key) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : "";
    }

    private String buildFailureReport(List<NoticeRagEvalResult> results) {
        return results.stream()
                .filter(result -> !result.isPass())
                .map(NoticeRagEvalResult::failureSummary)
                .reduce("\n", (left, right) -> left + "\n---\n" + right);
    }

}
