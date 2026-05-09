package kr.ac.knu.comit.notice.infra;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KnuCseNoticeCrawler {

    private static final String BASE_URL = "https://cse.knu.ac.kr/bbs/board.php";
    private static final String BOARD_TABLE = "sub5_1";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; ComitBot/1.0)";
    private static final int TIMEOUT_MS = 10_000;

    private static final DateTimeFormatter DETAIL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yy-MM-dd HH:mm");

    public List<NoticeListItem> crawlListPage(int page) throws IOException {
        String url = BASE_URL + "?bo_table=" + BOARD_TABLE + "&page=" + page;

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();

        Elements rows = doc.select("#bo_list tbody tr");
        return rows.stream()
                .map(this::parseListItem)
                .filter(Objects::nonNull)
                .toList();
    }

    public NoticeDetail crawlDetail(String wrId) throws IOException {
        String url = BASE_URL + "?bo_table=" + BOARD_TABLE + "&wr_id=" + wrId;

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();

        Element contentEl = doc.selectFirst("#bo_v_con");
        String content = contentEl != null ? contentEl.text() : "";

        Element dateEl = doc.selectFirst("#bo_v_info strong.if_date");
        LocalDateTime postedAt = null;
        if (dateEl != null) {
            try {
                postedAt = LocalDateTime.parse(dateEl.text().strip(), DETAIL_DATE_FORMAT);
            } catch (Exception ignored) {
            }
        }

        return new NoticeDetail(content, postedAt);
    }

    private NoticeListItem parseListItem(Element row) {
        Element titleEl = row.selectFirst("td.td_subject div.bo_tit a");
        if (titleEl == null) return null;

        String href = titleEl.absUrl("href");
        String wrId = UriComponentsBuilder.fromUriString(href)
                .build().getQueryParams().getFirst("wr_id");
        if (wrId == null) return null;

        String title = titleEl.text();

        Element authorEl = row.selectFirst("td.td_name span.sv_member");
        String author = authorEl != null ? authorEl.text() : null;

        Element dateEl = row.selectFirst("td.td_datetime");
        LocalDate postedDate = null;
        if (dateEl != null && !dateEl.text().isBlank()) {
            try {
                postedDate = LocalDate.parse(dateEl.text().strip());
            } catch (Exception ignored) {
            }
        }

        return new NoticeListItem(wrId, title, author, href, postedDate);
    }
}
