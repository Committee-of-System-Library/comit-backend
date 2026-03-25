package kr.ac.knu.comit.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_daily_visitor",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_daily_visitor",
                columnNames = {"post_id", "member_id", "viewed_on"}
        )
)
public class PostDailyVisitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "viewed_on", nullable = false, updatable = false)
    private LocalDate viewedOn;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PostDailyVisitor() {
    }

    public static PostDailyVisitor of(Long postId, Long memberId, LocalDate viewedOn) {
        PostDailyVisitor visitor = new PostDailyVisitor();
        visitor.postId = postId;
        visitor.memberId = memberId;
        visitor.viewedOn = viewedOn;
        visitor.createdAt = LocalDateTime.now();
        return visitor;
    }
}
