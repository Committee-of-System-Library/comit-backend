package kr.ac.knu.comit.global.docs;

final class FeatureSpecHtmlRenderer {

    record SpecInfo(String title, String summary, String href) {}

    private FeatureSpecHtmlRenderer() {}

    static SpecInfo buildSpecInfo(String fileName, String markdownContent) {
        String title = extractTitle(markdownContent);
        String summary = extractSummary(markdownContent);
        String href = "spec/" + fileName.replace(".md", ".html");
        return new SpecInfo(title, summary, href);
    }

    static String renderSpecPage(String markdownContent) {
        String title = extractTitle(markdownContent);
        String body = convertMarkdownToHtml(markdownContent);
        return wrapInPage(title, body);
    }

    private static String extractTitle(String markdown) {
        for (String line : markdown.split("\n")) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }
        return "기능 명세";
    }

    private static String extractSummary(String markdown) {
        boolean pastTitle = false;
        for (String line : markdown.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) { pastTitle = true; continue; }
            if (!pastTitle) continue;
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.equals("---")) continue;
            return trimmed
                    .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                    .replaceAll("`(.+?)`", "$1")
                    .replaceAll("\\[(.+?)]\\(.+?\\)", "$1");
        }
        return "";
    }

    static String convertMarkdownToHtml(String markdown) {
        String[] lines = markdown.split("\n");
        StringBuilder html = new StringBuilder();
        boolean inCodeBlock = false;
        String codeLang = "";
        boolean inList = false;
        boolean inOrderedList = false;
        StringBuilder codeBuffer = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    html.append("<pre><code class=\"language-").append(escapeHtml(codeLang)).append("\">")
                            .append(escapeHtml(codeBuffer.toString().stripTrailing()))
                            .append("</code></pre>\n");
                    codeBuffer.setLength(0);
                    inCodeBlock = false;
                    codeLang = "";
                } else {
                    closeList(html, inList, inOrderedList);
                    inList = false;
                    inOrderedList = false;
                    inCodeBlock = true;
                    codeLang = line.substring(3).trim();
                }
                continue;
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n");
                continue;
            }

            if (line.matches("^-{3,}$") || line.matches("^\\*{3,}$")) {
                closeList(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<hr>\n");
                continue;
            }

            if (line.startsWith("# ")) {
                closeList(html, inList, inOrderedList);
                inList = false; inOrderedList = false;
                html.append("<h1>").append(inline(line.substring(2).trim())).append("</h1>\n");
                continue;
            }
            if (line.startsWith("## ")) {
                closeList(html, inList, inOrderedList);
                inList = false; inOrderedList = false;
                html.append("<h2>").append(inline(line.substring(3).trim())).append("</h2>\n");
                continue;
            }
            if (line.startsWith("### ")) {
                closeList(html, inList, inOrderedList);
                inList = false; inOrderedList = false;
                html.append("<h3>").append(inline(line.substring(4).trim())).append("</h3>\n");
                continue;
            }
            if (line.startsWith("#### ")) {
                closeList(html, inList, inOrderedList);
                inList = false; inOrderedList = false;
                html.append("<h4>").append(inline(line.substring(5).trim())).append("</h4>\n");
                continue;
            }

            if (line.startsWith("> ")) {
                closeList(html, inList, inOrderedList);
                inList = false; inOrderedList = false;
                html.append("<blockquote>").append(inline(line.substring(2).trim())).append("</blockquote>\n");
                continue;
            }

            if (line.matches("^(- |\\* ).*")) {
                if (inOrderedList) { html.append("</ol>\n"); inOrderedList = false; }
                if (!inList) { html.append("<ul>\n"); inList = true; }
                String content = line.replaceFirst("^(- |\\* )", "").trim();
                html.append("<li>").append(inline(content)).append("</li>\n");
                continue;
            }

            if (line.matches("^\\d+\\. .*")) {
                if (inList) { html.append("</ul>\n"); inList = false; }
                if (!inOrderedList) { html.append("<ol>\n"); inOrderedList = true; }
                String content = line.replaceFirst("^\\d+\\. ", "").trim();
                html.append("<li>").append(inline(content)).append("</li>\n");
                continue;
            }

            if (line.trim().isEmpty()) {
                closeList(html, inList, inOrderedList);
                inList = false; inOrderedList = false;
                continue;
            }

            closeList(html, inList, inOrderedList);
            inList = false; inOrderedList = false;
            html.append("<p>").append(inline(line.trim())).append("</p>\n");
        }

        closeList(html, inList, inOrderedList);
        return html.toString();
    }

    private static void closeList(StringBuilder html, boolean inList, boolean inOrderedList) {
        if (inList) html.append("</ul>\n");
        if (inOrderedList) html.append("</ol>\n");
    }

    private static String inline(String text) {
        text = text.replaceAll("\\*\\*\\*(.+?)\\*\\*\\*", "<strong><em>$1</em></strong>");
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        text = text.replaceAll("`([^`]+)`", "<code>$1</code>");
        text = text.replaceAll("\\[([^]]+)]\\(([^)]+)\\)", "<a href=\"$2\">$1</a>");
        return text;
    }

    private static String wrapInPage(String title, String body) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s — COMIT Spec</title>
                    <style>
                        :root {
                            --bg: #f4f7fb;
                            --panel: #ffffff;
                            --text: #1b2a41;
                            --muted: #607086;
                            --line: #d8e2ef;
                            --accent: #3182f6;
                            --accent-soft: #e9f2ff;
                            --code-bg: #f1f5f9;
                        }
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            font-family: "SUIT", "Pretendard", system-ui, sans-serif;
                            background: radial-gradient(circle at top, #ffffff 0%%, var(--bg) 50%%, #edf3fa 100%%);
                            color: var(--text);
                            line-height: 1.75;
                        }
                        .shell { max-width: 860px; margin: 0 auto; padding: 48px 20px 72px; }
                        .back {
                            display: inline-flex; align-items: center; gap: 6px;
                            font-size: 0.85rem; color: var(--accent);
                            text-decoration: none; margin-bottom: 32px;
                        }
                        .back:hover { text-decoration: underline; }
                        .content {
                            background: var(--panel);
                            border: 1px solid var(--line);
                            border-radius: 20px;
                            padding: 40px 48px;
                            box-shadow: 0 8px 32px rgba(15,23,42,0.06);
                        }
                        h1 { font-size: 1.9rem; margin: 0 0 8px; border-bottom: 2px solid var(--line); padding-bottom: 16px; }
                        h2 { font-size: 1.25rem; margin: 36px 0 10px; color: var(--accent); }
                        h3 { font-size: 1.05rem; margin: 24px 0 8px; font-weight: 600; }
                        h4 { font-size: 0.95rem; margin: 16px 0 6px; color: var(--muted); font-weight: 600; }
                        p { margin: 0 0 12px; }
                        ul, ol { margin: 0 0 12px; padding-left: 22px; }
                        li { margin-bottom: 4px; }
                        code {
                            background: var(--code-bg); padding: 2px 6px;
                            border-radius: 4px; font-size: 0.875em;
                            font-family: "JetBrains Mono", "Fira Code", monospace;
                        }
                        pre {
                            background: var(--code-bg); border: 1px solid var(--line);
                            border-radius: 10px; padding: 16px 20px;
                            overflow-x: auto; margin: 0 0 16px;
                        }
                        pre code { background: none; padding: 0; font-size: 0.875rem; }
                        hr { border: none; border-top: 1px solid var(--line); margin: 28px 0; }
                        blockquote {
                            border-left: 3px solid var(--accent); margin: 0 0 12px;
                            padding: 8px 16px; background: var(--accent-soft);
                            border-radius: 0 8px 8px 0; color: var(--muted);
                        }
                        strong { font-weight: 600; }
                        a { color: var(--accent); }
                    </style>
                </head>
                <body>
                    <div class="shell">
                        <a class="back" href="../index.html">← API Docs 홈으로</a>
                        <div class="content">%s</div>
                    </div>
                </body>
                </html>
                """.formatted(escapeHtml(title), body);
    }

    static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
