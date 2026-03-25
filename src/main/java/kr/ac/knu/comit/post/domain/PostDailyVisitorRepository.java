package kr.ac.knu.comit.post.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface PostDailyVisitorRepository extends JpaRepository<PostDailyVisitor, Long> {

    @Modifying(clearAutomatically = true)
    @Query(
            value = """
                    INSERT IGNORE INTO post_daily_visitor (post_id, member_id, viewed_on, created_at)
                    VALUES (:postId, :memberId, :viewedOn, NOW())
                    """,
            nativeQuery = true
    )
    int insertIgnore(
            @Param("postId") Long postId,
            @Param("memberId") Long memberId,
            @Param("viewedOn") LocalDate viewedOn
    );
}
