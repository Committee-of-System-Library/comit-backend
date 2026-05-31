package kr.ac.knu.comit.comment.domain;

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
import kr.ac.knu.comit.global.exception.CommentErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.post.domain.Post;

@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private boolean hiddenByAdmin = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    protected Comment() {
    }

    public static Comment create(Post post, Member author, String content) {
        return create(post, null, author, content);
    }

    public static Comment reply(Post post, Comment parentComment, Member author, String content) {
        validateParentComment(post, parentComment);
        return create(post, parentComment, author, content);
    }

    private static Comment create(Post post, Comment parentComment, Member author, String content) {
        validateContent(content);

        Comment comment = new Comment();
        comment.post = post;
        comment.member = author;
        comment.parentComment = parentComment;
        comment.content = content.strip();
        comment.likeCount = 0;
        comment.createdAt = LocalDateTime.now();
        return comment;
    }

    public void update(String content) {
        validateContent(content);
        this.content = content.strip();
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isWrittenBy(Long memberId) {
        return member.getId().equals(memberId);
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public Long getParentCommentId() {
        return parentComment == null ? null : parentComment.getId();
    }

    public Member getMember() {
        return member;
    }

    public String getContent() {
        return content;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isReply() {
        return parentComment != null;
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

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(CommentErrorCode.INVALID_COMMENT_CONTENT);
        }
    }

    private static void validateParentComment(Post post, Comment parentComment) {
        if (parentComment == null
                || parentComment.isReply()
                || !parentComment.getPost().getId().equals(post.getId())) {
            throw new BusinessException(CommentErrorCode.INVALID_PARENT_COMMENT);
        }
    }
}
