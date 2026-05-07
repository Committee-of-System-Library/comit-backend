package kr.ac.knu.comit.notice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NoticeErrorCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "official_notice")
public class OfficialNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 원본 사이트의 고유 식별자. 크롤링 단계에서 중복 저장을 막기 위해 사용한다.
     * 수동 등록 시에는 null 허용.
     */
    @Column(length = 100, unique = true)
    private String sourceId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String author;

    @Column(length = 500)
    private String originalUrl;

    /**
     * 원본 사이트에 게시된 시각. 크롤링 전 수동 등록 시 null 허용.
     */
    private LocalDateTime postedAt;

    /**
     * AI가 생성한 요약문. 벡터 임베딩 단계에서 채워진다.
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    protected OfficialNotice() {
    }

    /**
     * 새 공지사항을 생성한다.
     */
    public static OfficialNotice create(String title, String content, String author,
                                        String originalUrl, LocalDateTime postedAt) {
        validateTitle(title);
        validateContent(content);

        OfficialNotice notice = new OfficialNotice();
        notice.title = title.strip();
        notice.content = content.strip();
        notice.author = author != null ? author.strip() : null;
        notice.originalUrl = originalUrl;
        notice.postedAt = postedAt;
        notice.createdAt = LocalDateTime.now();
        return notice;
    }

    /**
     * 공지사항의 수정 가능한 필드를 갱신한다.
     */
    public void update(String title, String content, String author,
                       String originalUrl, LocalDateTime postedAt) {
        validateTitle(title);
        validateContent(content);

        this.title = title.strip();
        this.content = content.strip();
        this.author = author != null ? author.strip() : null;
        this.originalUrl = originalUrl;
        this.postedAt = postedAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // ── getters ──────────────────────────────────────────────

    public Long getId() { return id; }

    public String getSourceId() { return sourceId; }

    public String getTitle() { return title; }

    public String getContent() { return content; }

    public String getAuthor() { return author; }

    public String getOriginalUrl() { return originalUrl; }

    public LocalDateTime getPostedAt() { return postedAt; }

    public String getSummary() { return summary; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── validation ───────────────────────────────────────────

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > OfficialNoticeConstraints.TITLE_MAX_LENGTH) {
            throw new BusinessException(NoticeErrorCode.INVALID_TITLE);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(NoticeErrorCode.INVALID_CONTENT);
        }
    }
}
