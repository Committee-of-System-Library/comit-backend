package kr.ac.knu.comit.inquiry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.domain.Member;

@Entity
@Table(name = "inquiry")
public class Inquiry {

    private static final int TITLE_MAX_LENGTH = 30;
    private static final int CONTENT_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Inquiry() {
    }

    public static Inquiry create(Member member, String title, String content) {
        if (member == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        Inquiry inquiry = new Inquiry();
        inquiry.member = member;
        inquiry.title = normalizeTitle(title);
        inquiry.content = normalizeContent(content);
        inquiry.createdAt = LocalDateTime.now();
        return inquiry;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private static String normalizeTitle(String title) {
        if (title == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        String normalized = title.strip();
        if (normalized.isEmpty() || normalized.length() > TITLE_MAX_LENGTH) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private static String normalizeContent(String content) {
        if (content == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        String normalized = content.strip();
        if (normalized.isEmpty() || normalized.length() > CONTENT_MAX_LENGTH) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }
}
