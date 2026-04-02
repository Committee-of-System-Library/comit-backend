package kr.ac.knu.comit.post.domain;

import jakarta.persistence.*;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.PostErrorCode;
import kr.ac.knu.comit.member.domain.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardType boardType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int viewCount = 0;

    /**
     * 태그는 게시글 생명주기를 따르며, 수정 시 전체 교체된다.
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> tags = new ArrayList<>();

    /**
     * 이미지는 게시글 생명주기를 따르며, 최대 5개까지 허용된다.
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PostImage> images = new ArrayList<>();

    @Column(nullable = false)
    private boolean hiddenByAdmin = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    protected Post() {
    }

    /**
     * 초기 태그와 이미지를 포함한 새 활성 게시글을 만든다.
     */
    public static Post create(Member author, BoardType boardType, String title, String content,
                              List<String> tagNames, List<String> imageUrls) {
        List<String> normalizedTagNames = normalizeTagNames(tagNames);
        List<String> normalizedImageUrls = normalizeImageUrls(imageUrls);
        validateTitle(title);
        validateContent(content);
        validateTagNames(normalizedTagNames);
        validateImageUrls(normalizedImageUrls);

        Post post = new Post();
        post.member = author;
        post.boardType = boardType;
        post.title = title;
        post.content = content;
        post.createdAt = LocalDateTime.now();

        normalizedTagNames.forEach(name -> post.tags.add(PostTag.of(post, name)));
        for (int i = 0; i < normalizedImageUrls.size(); i++) {
            post.images.add(PostImage.of(post, normalizedImageUrls.get(i), i));
        }
        return post;
    }

    /**
     * 게시글의 수정 가능한 상태를 전체 교체한다.
     *
     * @implNote 태그는 통째로 교체한다. 기존 row 정리는 orphan removal에 맡긴다.
     */
    public void update(String title, String content, List<String> tagNames) {
        List<String> normalizedTagNames = normalizeTagNames(tagNames);
        validateTitle(title);
        validateContent(content);
        validateTagNames(normalizedTagNames);

        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
        this.tags.clear();
        normalizedTagNames.forEach(name -> this.tags.add(PostTag.of(this, name)));
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 주어진 회원이 이 게시글의 작성자인지 확인한다.
     */
    public boolean isWrittenBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public BoardType getBoardType() {
        return boardType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getViewCount() {
        return viewCount;
    }

    public List<PostTag> getTags() {
        return tags;
    }

    public List<PostImage> getImages() {
        return images;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void hideByAdmin() {
        this.hiddenByAdmin = true;
    }

    public void restoreByAdmin() {
        this.hiddenByAdmin = false;
    }

    public boolean isHiddenByAdmin() {
        return hiddenByAdmin;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > PostConstraints.TITLE_MAX_LENGTH) {
            throw new BusinessException(PostErrorCode.INVALID_TITLE);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > PostConstraints.CONTENT_MAX_LENGTH) {
            throw new BusinessException(PostErrorCode.INVALID_CONTENT);
        }
    }

    private static void validateTagNames(List<String> tagNames) {
        if (tagNames == null) return;
        if (tagNames.size() > PostConstraints.TAG_MAX_COUNT) {
            throw new BusinessException(PostErrorCode.TAG_LIMIT_EXCEEDED);
        }
        tagNames.forEach(PostTag::validateName);
    }

    private static void validateImageUrls(List<String> imageUrls) {
        if (imageUrls.size() > PostConstraints.IMAGE_MAX_COUNT) {
            throw new BusinessException(PostErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
    }

    private static List<String> normalizeTagNames(List<String> tagNames) {
        return tagNames == null ? List.of() : tagNames;
    }

    private static List<String> normalizeImageUrls(List<String> imageUrls) {
        return imageUrls == null ? List.of() : imageUrls;
    }
}
