package kr.ac.knu.comit.comment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "comment_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_like",
                columnNames = {"comment_id", "member_id"}
        )
)
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CommentLike() {
    }

    public static CommentLike of(Long commentId, Long memberId) {
        CommentLike commentLike = new CommentLike();
        commentLike.commentId = commentId;
        commentLike.memberId = memberId;
        commentLike.createdAt = LocalDateTime.now();
        return commentLike;
    }
}
