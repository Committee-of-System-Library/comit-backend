package kr.ac.knu.comit.comment.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Modifying(clearAutomatically = true)
    @Query(
            value = """
                    INSERT IGNORE INTO comment_like (comment_id, member_id, created_at)
                    VALUES (:commentId, :memberId, NOW())
                    """,
            nativeQuery = true
    )
    int insertIgnore(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM CommentLike cl
            WHERE cl.commentId = :commentId
              AND cl.memberId = :memberId
            """)
    void deleteByCommentIdAndMemberId(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

    @Query("""
            SELECT cl.commentId
            FROM CommentLike cl
            WHERE cl.memberId = :memberId
              AND cl.commentId IN :commentIds
            """)
    List<Long> findLikedCommentIds(
            @Param("memberId") Long memberId,
            @Param("commentIds") List<Long> commentIds
    );
}
