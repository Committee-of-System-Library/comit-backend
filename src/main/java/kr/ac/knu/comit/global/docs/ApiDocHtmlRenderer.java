package kr.ac.knu.comit.global.docs;

import java.util.List;

final class ApiDocHtmlRenderer {

    private ApiDocHtmlRenderer() {
    }

    static String renderIndexHtml() {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>COMIT API Docs</title>
                    <style>
                        :root {
                            --bg: #f4f7fb;
                            --panel: #ffffff;
                            --text: #1b2a41;
                            --muted: #607086;
                            --line: #d8e2ef;
                            --accent: #3182f6;
                            --accent-soft: #e9f2ff;
                            --code: #0f172a;
                        }
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            font-family: "SUIT", "Pretendard", system-ui, sans-serif;
                            background: radial-gradient(circle at top, #ffffff 0%, var(--bg) 50%, #edf3fa 100%);
                            color: var(--text);
                        }
                        .shell {
                            max-width: 1120px;
                            margin: 0 auto;
                            padding: 48px 20px 72px;
                        }
                        .hero {
                            margin-bottom: 28px;
                            padding: 28px;
                            border: 1px solid var(--line);
                            border-radius: 28px;
                            background: linear-gradient(135deg, rgba(49,130,246,0.12), rgba(255,255,255,0.96));
                            box-shadow: 0 24px 80px rgba(15, 23, 42, 0.08);
                        }
                        .hero h1 {
                            margin: 0 0 8px;
                            font-size: clamp(2rem, 3vw, 2.8rem);
                        }
                        .hero p {
                            margin: 0;
                            color: var(--muted);
                            line-height: 1.6;
                        }
                        .grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                            gap: 16px;
                        }
                        .card {
                            display: block;
                            padding: 22px;
                            border-radius: 24px;
                            border: 1px solid var(--line);
                            background: var(--panel);
                            text-decoration: none;
                            color: inherit;
                            box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
                            transition: transform 140ms ease, box-shadow 140ms ease, border-color 140ms ease;
                        }
                        .card:hover {
                            transform: translateY(-3px);
                            border-color: rgba(49,130,246,0.45);
                            box-shadow: 0 24px 48px rgba(49,130,246,0.14);
                        }
                        .pill {
                            display: inline-block;
                            margin-bottom: 12px;
                            padding: 6px 10px;
                            border-radius: 999px;
                            background: var(--accent-soft);
                            color: var(--accent);
                            font-size: 0.8rem;
                            font-weight: 700;
                        }
                        .card h2 {
                            margin: 0 0 10px;
                            font-size: 1.1rem;
                        }
                        .card p {
                            margin: 0 0 12px;
                            color: var(--muted);
                            line-height: 1.5;
                        }
                        .meta {
                            color: var(--muted);
                            font-size: 0.92rem;
                        }
                    </style>
                </head>
                <body>
                    <main class="shell">
                        <section class="hero">
                            <h1>COMIT API Docs</h1>
                            <p>@ApiContract 인터페이스에서 추출한 정적 API 문서입니다. 각 페이지에서 엔드포인트, 요청/응답 필드, 예시 JSON을 확인할 수 있습니다.</p>
                        </section>
                        <section class="grid" id="doc-grid"></section>
                    </main>
                    <script src="./index.js"></script>
                    <script>
                        const docs = window.API_DOCS ?? [];
                        const grid = document.getElementById('doc-grid');
                        docs.forEach((doc) => {
                            const link = document.createElement('a');
                            link.className = 'card';
                            link.href = typeof doc.href === 'string' ? doc.href : '#';

                            const pill = document.createElement('span');
                            pill.className = 'pill';
                            pill.textContent = doc.sectionPath || 'root';

                            const title = document.createElement('h2');
                            title.textContent = doc.title || '';

                            const summary = document.createElement('p');
                            summary.textContent = doc.summary || '';

                            const meta = document.createElement('div');
                            meta.className = 'meta';
                            meta.textContent = `${doc.endpointCount ?? 0} endpoint(s)`;

                            link.append(pill, title, summary, meta);
                            grid.appendChild(link);
                        });
                    </script>
                </body>
                </html>
                """;
    }

    static String renderIndexScript(List<GeneratedApiDocument> documents) {
        String items = documents.stream()
                .map(document -> {
                    String summary = document.endpoints().isEmpty()
                            ? "-"
                            : firstLine(document.endpoints().getFirst().description(), document.endpoints().getFirst().summary());
                    return """
                            {
                              title: "%s",
                              summary: "%s",
                              href: "./%s",
                              endpointCount: %d,
                              sectionPath: "%s"
                            }""".formatted(
                            escapeJs(document.title()),
                            escapeJs(summary),
                            escapeJs(document.relativeOutputPath()),
                            document.endpoints().size(),
                            escapeJs(document.sectionPath())
                    );
                })
                .reduce((left, right) -> left + ",\n" + right)
                .orElse("");

        return "window.API_DOCS = [\n" + items + "\n];\n";
    }

    static String renderDocument(GeneratedApiDocument document) {
        String backLink = document.sectionPath().isBlank()
                ? "./index.html"
                : "../".repeat(document.sectionPath().split("/").length) + "index.html";
        String navigation = document.endpoints().stream()
                .map(endpoint -> """
                        <a href="#%s">
                            <strong>%s</strong>
                            <span>%s %s</span>
                        </a>
                        """.formatted(
                        escapeHtml(endpoint.methodName()),
                        escapeHtml(endpoint.summary()),
                        escapeHtml(endpoint.httpMethod()),
                        escapeHtml(endpoint.path())
                ))
                .reduce((left, right) -> left + right)
                .orElse("");

        String sections = document.endpoints().stream()
                .map(ApiDocHtmlRenderer::renderEndpoint)
                .reduce((left, right) -> left + right)
                .orElse("");

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>__TITLE__</title>
                    <style>
                        :root {
                            --bg: #f4f7fb;
                            --panel: #ffffff;
                            --panel-soft: #f9fbff;
                            --text: #1b2a41;
                            --muted: #607086;
                            --line: #d8e2ef;
                            --accent: #3182f6;
                            --accent-soft: #e9f2ff;
                            --success-bg: #effaf5;
                            --success-text: #0f8a5f;
                            --code: #0f172a;
                        }
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            font-family: "SUIT", "Pretendard", system-ui, sans-serif;
                            color: var(--text);
                            background:
                                radial-gradient(circle at top left, rgba(49, 130, 246, 0.14), transparent 36%),
                                linear-gradient(180deg, #fafdff 0%, var(--bg) 45%, #edf3fa 100%);
                        }
                        a { color: inherit; }
                        .layout {
                            max-width: 1240px;
                            margin: 0 auto;
                            padding: 32px 20px 72px;
                            display: grid;
                            gap: 24px;
                            grid-template-columns: 280px minmax(0, 1fr);
                        }
                        .sidebar {
                            position: sticky;
                            top: 24px;
                            height: fit-content;
                            padding: 24px;
                            border: 1px solid var(--line);
                            border-radius: 28px;
                            background: rgba(255, 255, 255, 0.9);
                            box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
                        }
                        .sidebar h1 {
                            margin: 0 0 8px;
                            font-size: 1.5rem;
                        }
                        .sidebar p {
                            margin: 0 0 20px;
                            color: var(--muted);
                            line-height: 1.5;
                        }
                        .sidebar nav {
                            display: grid;
                            gap: 10px;
                        }
                        .sidebar nav a {
                            padding: 14px;
                            border-radius: 18px;
                            text-decoration: none;
                            background: var(--panel-soft);
                            border: 1px solid transparent;
                        }
                        .sidebar nav a:hover {
                            border-color: rgba(49,130,246,0.3);
                            background: var(--accent-soft);
                        }
                        .sidebar nav strong {
                            display: block;
                            margin-bottom: 4px;
                        }
                        .sidebar nav span {
                            color: var(--muted);
                            font-size: 0.92rem;
                        }
                        .content {
                            display: grid;
                            gap: 18px;
                        }
                        .endpoint {
                            padding: 28px;
                            border-radius: 28px;
                            border: 1px solid var(--line);
                            background: rgba(255,255,255,0.92);
                            box-shadow: 0 20px 50px rgba(15, 23, 42, 0.06);
                        }
                        .eyebrow {
                            display: inline-flex;
                            align-items: center;
                            gap: 10px;
                            margin-bottom: 14px;
                            padding: 8px 12px;
                            border-radius: 999px;
                            background: var(--accent-soft);
                            color: var(--accent);
                            font-weight: 700;
                        }
                        .endpoint h2 {
                            margin: 0 0 8px;
                            font-size: clamp(1.4rem, 2vw, 1.8rem);
                        }
                        .endpoint p {
                            margin: 0 0 18px;
                            color: var(--muted);
                            line-height: 1.6;
                        }
                        .path-box {
                            margin-bottom: 18px;
                            padding: 16px 18px;
                            border-radius: 20px;
                            background: #0f172a;
                            color: #e2e8f0;
                            font-family: "SFMono-Regular", "Menlo", monospace;
                            overflow: auto;
                        }
                        .method {
                            color: #8bc4ff;
                            font-weight: 800;
                            margin-right: 10px;
                        }
                        .section-grid {
                            display: grid;
                            gap: 18px;
                        }
                        .card {
                            border: 1px solid var(--line);
                            border-radius: 22px;
                            padding: 20px;
                            background: var(--panel-soft);
                        }
                        .card h3 {
                            margin: 0 0 14px;
                            font-size: 1rem;
                        }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                        }
                        th, td {
                            padding: 12px 10px;
                            border-bottom: 1px solid var(--line);
                            text-align: left;
                            vertical-align: top;
                        }
                        th {
                            color: var(--muted);
                            font-size: 0.9rem;
                            font-weight: 700;
                        }
                        td code {
                            color: var(--code);
                            font-family: "SFMono-Regular", "Menlo", monospace;
                        }
                        .type-chip {
                            display: inline-flex;
                            align-items: center;
                            padding: 4px 10px;
                            border-radius: 999px;
                            border: 1px solid transparent;
                            font-size: 0.82rem;
                            font-weight: 700;
                            line-height: 1.2;
                            white-space: nowrap;
                        }
                        .type-string {
                            background: #fff1e8;
                            border-color: #ffd8bf;
                            color: #c25b12;
                        }
                        .type-number {
                            background: #edf7ff;
                            border-color: #cfe7ff;
                            color: #1565c0;
                        }
                        .type-boolean {
                            background: #eefcf4;
                            border-color: #cfeeda;
                            color: #1f7a4d;
                        }
                        .type-datetime {
                            background: #f3efff;
                            border-color: #ddd2ff;
                            color: #6b46c1;
                        }
                        .type-collection {
                            background: #ecfbff;
                            border-color: #c8edf6;
                            color: #0f6e83;
                        }
                        .type-custom {
                            background: #f5f7fa;
                            border-color: #dde4ee;
                            color: #43536a;
                        }
                        .badge-required {
                            display: inline-block;
                            padding: 4px 8px;
                            border-radius: 999px;
                            background: var(--success-bg);
                            color: var(--success-text);
                            font-size: 0.78rem;
                            font-weight: 800;
                        }
                        .badge-optional {
                            display: inline-block;
                            padding: 4px 8px;
                            border-radius: 999px;
                            background: #eef2f7;
                            color: #556274;
                            font-size: 0.78rem;
                            font-weight: 800;
                        }
                        pre {
                            margin: 0;
                            padding: 18px;
                            overflow: auto;
                            border-radius: 18px;
                            background: #0f172a;
                            color: #e2e8f0;
                            font-size: 0.9rem;
                            line-height: 1.6;
                        }
                        .empty {
                            color: var(--muted);
                            margin: 0;
                        }
                        .back-link {
                            display: inline-block;
                            margin-bottom: 18px;
                            color: var(--accent);
                            text-decoration: none;
                            font-weight: 700;
                        }
                        @media (max-width: 960px) {
                            .layout {
                                grid-template-columns: 1fr;
                            }
                            .sidebar {
                                position: static;
                            }
                        }
                    </style>
                </head>
                <body>
                    <main class="layout">
                        <aside class="sidebar">
                            <a class="back-link" href="__BACK_LINK__">← API Index</a>
                            <h1>__TITLE__</h1>
                            <p>인터페이스 기반으로 추출한 엔드포인트 문서입니다.</p>
                            <nav>__NAVIGATION__</nav>
                        </aside>
                        <section class="content">__SECTIONS__</section>
                    </main>
                </body>
                </html>
                """
                .replace("__BACK_LINK__", backLink)
                .replace("__TITLE__", escapeHtml(document.title()))
                .replace("__NAVIGATION__", navigation)
                .replace("__SECTIONS__", sections);
    }

    private static String renderEndpoint(GeneratedEndpoint endpoint) {
        return """
                <article class="endpoint" id="%s">
                    <div class="eyebrow">%s</div>
                    <h2>%s</h2>
                    <p>%s</p>
                    <div class="path-box"><span class="method">%s</span>%s</div>
                    <div class="section-grid">%s</div>
                </article>
                """.formatted(
                escapeHtml(endpoint.methodName()),
                escapeHtml(endpoint.httpMethod()),
                escapeHtml(endpoint.summary()),
                escapeHtml(firstLine(endpoint.description(), endpoint.summary())),
                escapeHtml(endpoint.httpMethod()),
                escapeHtml(endpoint.path()),
                renderEndpointSections(endpoint)
        );
    }

    private static String firstLine(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback == null ? "" : fallback;
    }

    private static String renderEndpointSections(GeneratedEndpoint endpoint) {
        StringBuilder sections = new StringBuilder();
        sections.append(renderFieldCard("경로 변수", endpoint.pathParameters()));
        sections.append(renderFieldCard("쿼리 파라미터", endpoint.queryParameters()));
        sections.append(renderFieldCard("요청 바디 필드", endpoint.requestFields()));
        sections.append(renderFieldCard("응답 바디 필드", endpoint.responseFields()));
        sections.append(renderErrorFieldCard(endpoint.errors()));
        sections.append(renderErrorCodeCard(endpoint.errors()));
        sections.append(renderExampleCard("요청 예시", endpoint.requestExample()));
        sections.append(renderExampleCard("응답 예시", endpoint.responseExample()));
        sections.append(renderExampleCard("에러 예시", endpoint.errorExample()));
        return sections.toString();
    }

    private static String renderErrorFieldCard(List<ErrorDoc> errors) {
        if (errors == null || errors.isEmpty()) {
            return "";
        }
        return renderFieldCard("에러 응답 필드", List.of(
                new FieldDoc("type", "String", true, "문제 유형을 식별하는 URI path입니다."),
                new FieldDoc("title", "String", true, "HTTP 상태를 요약한 제목입니다."),
                new FieldDoc("status", "Integer", true, "HTTP 상태 코드입니다."),
                new FieldDoc("detail", "String", true, "클라이언트에 노출할 에러 설명입니다."),
                new FieldDoc("instance", "String", true, "에러가 발생한 요청 경로입니다."),
                new FieldDoc("errorCode", "String", true, "프론트엔드 분기에 사용하는 안정적인 에러 코드입니다."),
                new FieldDoc("invalidFields", "List<ProblemFieldViolation>", false, "validation 에러에서만 포함됩니다. 필드명과 검증 메시지 목록입니다."),
                new FieldDoc("errorTrackingId", "String", false, "서버 내부 오류에서만 포함됩니다. 로그 추적용 식별자입니다."),
                new FieldDoc("timestamp", "OffsetDateTime", true, "에러 응답 생성 시각입니다.")
        ));
    }

    private static String renderErrorCodeCard(List<ErrorDoc> errors) {
        if (errors == null || errors.isEmpty()) {
            return "";
        }
        return """
                <section class="card">
                    <h3>에러 코드</h3>
                    %s
                </section>
                """.formatted(renderErrorTable(errors));
    }

    private static String renderFieldCard(String title, List<FieldDoc> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        return """
                <section class="card">
                    <h3>%s</h3>
                    %s
                </section>
                """.formatted(escapeHtml(title), renderFieldTable(fields));
    }

    private static String renderExampleCard(String title, String example) {
        if (example == null || example.isBlank()) {
            return "";
        }
        return """
                <section class="card">
                    <h3>%s</h3>
                    %s
                </section>
                """.formatted(escapeHtml(title), renderCodeBlock(example));
    }

    private static String renderFieldTable(List<FieldDoc> fields) {
        if (fields.isEmpty()) {
            return "<p class=\"empty\">문서화할 필드가 없습니다.</p>";
        }

        String rows = fields.stream()
                .map(field -> """
                        <tr>
                            <td><code>%s</code></td>
                            <td><span class="type-chip %s">%s</span></td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                        """.formatted(
                        escapeHtml(field.name()),
                        classifyType(field.type()),
                        escapeHtml(field.type()),
                        field.required()
                                ? "<span class=\"badge-required\">required</span>"
                                : "<span class=\"badge-optional\">optional</span>",
                        escapeHtml(field.description())
                ))
                .reduce((left, right) -> left + right)
                .orElse("");

        return """
                <table>
                    <thead>
                        <tr>
                            <th>필드명</th>
                            <th>타입</th>
                            <th>필수 여부</th>
                            <th>설명</th>
                        </tr>
                    </thead>
                    <tbody>%s</tbody>
                </table>
                """.formatted(rows);
    }

    private static String renderErrorTable(List<ErrorDoc> errors) {
        String rows = errors.stream()
                .map(error -> """
                        <tr>
                            <td><code>%s</code></td>
                            <td>%d</td>
                            <td><code>%s</code></td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                        """.formatted(
                        escapeHtml(error.errorCode()),
                        error.status(),
                        escapeHtml(error.type()),
                        escapeHtml(error.detail()),
                        escapeHtml(error.when())
                ))
                .reduce((left, right) -> left + right)
                .orElse("");

        return """
                <table>
                    <thead>
                        <tr>
                            <th>errorCode</th>
                            <th>HTTP 상태</th>
                            <th>type</th>
                            <th>detail</th>
                            <th>발생 조건</th>
                        </tr>
                    </thead>
                    <tbody>%s</tbody>
                </table>
                """.formatted(rows);
    }

    private static String renderCodeBlock(String example) {
        if (example == null || example.isBlank()) {
            return "<p class=\"empty\">예시가 없습니다.</p>";
        }
        return "<pre><code>" + escapeHtml(example) + "</code></pre>";
    }

    private static String classifyType(String type) {
        if (type == null || type.isBlank()) {
            return "type-custom";
        }

        if (type.contains("[]")
                || type.startsWith("List<")
                || type.startsWith("Set<")
                || type.startsWith("Collection<")
                || type.startsWith("Map<")) {
            return "type-collection";
        }

        if (type.equals("String") || type.equals("CharSequence")) {
            return "type-string";
        }

        if (type.equals("Boolean") || type.equals("boolean")) {
            return "type-boolean";
        }

        if (type.equals("LocalDate")
                || type.equals("LocalDateTime")
                || type.equals("OffsetDateTime")
                || type.equals("Instant")
                || type.equals("ZonedDateTime")) {
            return "type-datetime";
        }

        if (type.equals("byte") || type.equals("Byte")
                || type.equals("short") || type.equals("Short")
                || type.equals("int") || type.equals("Integer")
                || type.equals("long") || type.equals("Long")
                || type.equals("float") || type.equals("Float")
                || type.equals("double") || type.equals("Double")
                || type.equals("BigDecimal") || type.equals("BigInteger")) {
            return "type-number";
        }

        return "type-custom";
    }

    private static String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeJs(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
