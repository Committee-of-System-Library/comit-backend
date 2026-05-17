package kr.ac.knu.comit.notice.infrastructure;

final class NoticeDocumentText {

    private NoticeDocumentText() {
    }

    static String format(String title, String content) {
        return """
                제목: %s
                
                본문:
                %s
                """.formatted(title, content);
    }
}
