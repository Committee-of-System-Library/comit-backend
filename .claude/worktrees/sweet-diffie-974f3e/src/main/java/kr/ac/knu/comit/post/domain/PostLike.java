package kr.ac.knu.comit.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * post_like 테이블의 UNIQUE(post_id, member_id) 제약이
 * 동시 좋아요 중복을 DB 레벨에서 차단하는 핵심 장치.
 */
@Entity
@Table(name = "post_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_like",
                columnNames = {"post_id", "member_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostLike of(Long postId, Long memberId) {
        PostLike like = new PostLike();
        like.postId = postId;
        like.memberId = memberId;
        like.createdAt = LocalDateTime.now();
        return like;
    }
}
