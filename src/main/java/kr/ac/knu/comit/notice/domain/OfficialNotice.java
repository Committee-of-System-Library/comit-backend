package kr.ac.knu.comit.notice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.NoticeErrorCode;

@Entity
@Table(name = "official_notice")
public class OfficialNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 원본 게시판의 wr_id. 크롤러 중복 저장 방지용. */
    @Column(length = 20, unique = true)
    private String wrId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String author;

    @Column(length = 500)
    private String originalUrl;

    private LocalDateTime postedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    protected OfficialNotice() {
    }

    public static OfficialNotice create(String wrId, String title, String content, String author,
                                        String originalUrl, LocalDateTime postedAt, String summary) {
        validateTitle(title);
        validateContent(content);

        OfficialNotice notice = new OfficialNotice();
        notice.wrId = wrId;
        notice.title = title.strip();
        notice.content = content.strip();
        notice.author = author != null ? author.strip() : null;
        notice.originalUrl = originalUrl;
        notice.postedAt = postedAt;
        notice.summary = summary;
        notice.createdAt = LocalDateTime.now();
        return notice;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > 300) {
            throw new BusinessException(NoticeErrorCode.INVALID_TITLE);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(NoticeErrorCode.INVALID_CONTENT);
        }
    }

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

    public Long getId() {
        return id;
    }

    public String getWrId() {
        return wrId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
