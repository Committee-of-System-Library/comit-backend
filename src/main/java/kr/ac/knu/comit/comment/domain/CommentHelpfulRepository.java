package kr.ac.knu.comit.comment.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentHelpfulRepository extends JpaRepository<CommentHelpful, Long> {

    @Modifying(clearAutomatically = true)
    @Query(
            value = """
                    INSERT IGNORE INTO comment_helpful (comment_id, member_id, created_at)
                    VALUES (:commentId, :memberId, NOW())
                    """,
            nativeQuery = true
    )
    int insertIgnore(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM CommentHelpful ch
            WHERE ch.commentId = :commentId
              AND ch.memberId = :memberId
            """)
    void deleteByCommentIdAndMemberId(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

    @Query("""
            SELECT ch.commentId
            FROM CommentHelpful ch
            WHERE ch.memberId = :memberId
              AND ch.commentId IN :commentIds
            """)
    List<Long> findHelpfulCommentIds(
            @Param("memberId") Long memberId,
            @Param("commentIds") List<Long> commentIds
    );
}
