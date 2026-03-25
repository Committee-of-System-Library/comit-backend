package kr.ac.knu.comit.comment.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.member
            WHERE c.id = :commentId
              AND c.deletedAt IS NULL
            """)
    Optional<Comment> findActiveById(@Param("commentId") Long commentId);

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.member
            WHERE c.post.id = :postId
              AND c.deletedAt IS NULL
              AND c.parentComment IS NULL
            ORDER BY c.helpfulCount DESC, c.id ASC
            """)
    List<Comment> findActiveTopLevelByPostId(@Param("postId") Long postId);

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.member
            JOIN FETCH c.parentComment parent
            WHERE c.post.id = :postId
              AND c.deletedAt IS NULL
              AND parent.deletedAt IS NULL
            ORDER BY parent.id ASC, c.id ASC
            """)
    List<Comment> findActiveRepliesByPostId(@Param("postId") Long postId);

    @Query("""
            SELECT c FROM Comment c
            WHERE c.parentComment.id = :parentCommentId
              AND c.deletedAt IS NULL
            ORDER BY c.id ASC
            """)
    List<Comment> findActiveRepliesByParentCommentId(@Param("parentCommentId") Long parentCommentId);

    @Query("""
            SELECT c.post.id AS postId, COUNT(c) AS commentCount
            FROM Comment c
            WHERE c.post.id IN :postIds
              AND c.deletedAt IS NULL
            GROUP BY c.post.id
            """)
    List<CommentCountView> countActiveByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Comment c
            SET c.helpfulCount = c.helpfulCount + 1
            WHERE c.id = :commentId
              AND c.deletedAt IS NULL
            """)
    void incrementHelpfulCount(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Comment c
            SET c.helpfulCount = c.helpfulCount - 1
            WHERE c.id = :commentId
              AND c.deletedAt IS NULL
              AND c.helpfulCount > 0
            """)
    void decrementHelpfulCount(@Param("commentId") Long commentId);

    interface CommentCountView {
        Long getPostId();
        long getCommentCount();
    }
}
