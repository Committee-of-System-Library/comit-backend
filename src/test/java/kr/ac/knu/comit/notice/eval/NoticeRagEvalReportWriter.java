package kr.ac.knu.comit.notice.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class NoticeRagEvalReportWriter {

    private static final Path REPORT_DIR = Path.of("build", "reports", "notice-rag-eval");

    private final ObjectMapper objectMapper;

    NoticeRagEvalReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void write(List<NoticeRagEvalResult> results) throws IOException {
        Files.createDirectories(REPORT_DIR);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(REPORT_DIR.resolve("notice-rag-eval-report.json").toFile(), results);
        Files.writeString(REPORT_DIR.resolve("notice-rag-eval-report.md"), buildMarkdownReport(results));
    }

    private String buildMarkdownReport(List<NoticeRagEvalResult> results) {
        long passCount = results.stream().filter(NoticeRagEvalResult::isPass).count();
        long failCount = results.size() - passCount;

        StringBuilder builder = new StringBuilder();
        builder.append("# Notice RAG Eval Report\n\n")
                .append("- total: ").append(results.size()).append('\n')
                .append("- pass: ").append(results.stream().filter(result -> result.status() == NoticeRagEvalResult.EvalStatus.PASS).count()).append('\n')
                .append("- passWithWarning: ").append(results.stream().filter(result -> result.status() == NoticeRagEvalResult.EvalStatus.PASS_WITH_WARNING).count()).append('\n')
                .append("- fail: ").append(failCount).append("\n\n")
                .append("| caseId | status | transformedQuery | selectedSourceCount | appliedLimit | answerSourceCount | answerSources | answerTokens | warnings | failReason |\n")
                .append("|---|---|---|---:|---:|---:|---|---:|---|---|\n");

        for (NoticeRagEvalResult result : results) {
            builder.append("| ")
                    .append(escape(result.caseId()))
                    .append(" | ")
                    .append(result.status())
                    .append(" | ")
                    .append(escape(result.transformedQuery()))
                    .append(" | ")
                    .append(result.selectedNoticeIds().size())
                    .append(" | ")
                    .append(result.appliedSourceLimit())
                    .append(" | ")
                    .append(result.sourceNoticeIds().size())
                    .append(" | ")
                    .append(escape(String.join("<br>", result.sourceTitles())))
                    .append(" | ")
                    .append(result.answerTotalTokens())
                    .append(" | ")
                    .append(escape(String.join(", ", result.warnings())))
                    .append(" | ")
                    .append(escape(result.failReason()))
                    .append(" |\n");
        }

        builder.append("\n## Failed Cases\n\n");
        results.stream()
                .filter(result -> !result.isPass())
                .forEach(result -> builder.append("### ")
                        .append(result.caseId())
                        .append("\n\n```text\n")
                        .append(result.failureSummary())
                        .append("\n```\n\n"));

        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "\\|")
                .replace("\n", "<br>");
    }
}
