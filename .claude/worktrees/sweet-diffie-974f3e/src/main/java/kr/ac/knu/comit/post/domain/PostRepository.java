package kr.ac.knu.comit.post.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    /**
     * 작성자 정보와 함께 활성 게시글 하나를 조회한다.
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.member LEFT JOIN FETCH p.tags WHERE p.id = :id AND p.deletedAt IS NULL AND p.hiddenByAdmin = false")
    Optional<Post> findActiveById(@Param("id") Long id);

    /**
     * 주어진 cursor 뒤의 다음 게시글 페이지를 조회한다.
     *
     * @implNote offset을 스캔하지 않도록 {@code id < cursorId} 조건으로
     * 게시판 인덱스를 따라가며 페이지를 읽는다.
     */
    @Query("""
            SELECT p FROM Post p
            JOIN FETCH p.member
            WHERE p.boardType = :boardType
              AND p.deletedAt IS NULL
              AND p.hiddenByAdmin = false
              AND p.id < :cursorId
            ORDER BY p.id DESC
            """)
    List<Post> findByCursor(@Param("boardType") BoardType boardType,
                            @Param("cursorId") Long cursorId,
                            Pageable pageable);

    /**
     * 요청한 게시판의 첫 페이지를 조회한다.
     */
    @Query("""
            SELECT p FROM Post p
            JOIN FETCH p.member
            WHERE p.boardType = :boardType
              AND p.deletedAt IS NULL
              AND p.hiddenByAdmin = false
            ORDER BY p.id DESC
            """)
    List<Post> findFirstPage(@Param("boardType") BoardType boardType,
                             Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Post p
            JOIN FETCH p.member
            LEFT JOIN FETCH p.tags
            WHERE p.id IN :postIds
              AND p.deletedAt IS NULL
              AND p.hiddenByAdmin = false
            """)
    List<Post> findActiveWithMemberAndTagsByIds(@Param("postIds") List<Long> postIds);

    /**
     * Admin: 숨김 포함, 삭제 제외한 전체 게시글을 페이징 조회한다.
     */
    @Query("""
            SELECT p FROM Post p
            JOIN FETCH p.member
            WHERE p.deletedAt IS NULL
              AND (:boardType IS NULL OR p.boardType = :boardType)
            ORDER BY p.id DESC
            """)
    Page<Post> findAllActiveForAdmin(
            @Param("boardType") BoardType boardType,
            Pageable pageable
    );

    /**
     * Admin: 숨김 포함한 단건 게시글을 조회한다.
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.member WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findActiveByIdForAdmin(@Param("id") Long id);

    @Query(
            value = """
                    SELECT p.id AS postId,
                           (
                               LOG(1 + COALESCE(pl.weighted_like_count, 0)) * :likeWeight
                               + LOG(1 + COALESCE(c.weighted_comment_count, 0)) * :commentWeight
                               + LOG(1 + COALESCE(pdv.weighted_visitor_count, 0)) * :visitorWeight
                           ) AS score
                    FROM post p
                    LEFT JOIN (
                        SELECT post_id,
                               SUM(EXP(-:decayRate * DATEDIFF(:today, DATE(created_at)))) AS weighted_like_count,
                               COUNT(*) AS raw_like_count
                        FROM post_like
                        WHERE created_at >= :startDateTime
                        GROUP BY post_id
                    ) pl ON pl.post_id = p.id
                    LEFT JOIN (
                        SELECT post_id,
                               SUM(EXP(-:decayRate * DATEDIFF(:today, DATE(created_at)))) AS weighted_comment_count,
                               COUNT(*) AS raw_comment_count
                        FROM `comment`
                        WHERE deleted_at IS NULL
                          AND created_at >= :startDateTime
                        GROUP BY post_id
                    ) c ON c.post_id = p.id
                    LEFT JOIN (
                        SELECT post_id,
                               SUM(EXP(-:decayRate * DATEDIFF(:today, viewed_on))) AS weighted_visitor_count,
                               COUNT(DISTINCT member_id) AS raw_visitor_count
                        FROM post_daily_visitor
                        WHERE viewed_on >= :startDate
                        GROUP BY post_id
                    ) pdv ON pdv.post_id = p.id
                    WHERE p.deleted_at IS NULL
                      AND (:excludedBoardTypesEmpty = true OR p.board_type NOT IN (:excludedBoardTypes))
                      AND (
                          COALESCE(pl.raw_like_count, 0)
                          + COALESCE(c.raw_comment_count, 0)
                          + COALESCE(pdv.raw_visitor_count, 0)
                      ) >= :minReactions
                    ORDER BY score DESC, p.created_at DESC, p.id DESC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<HotPostScoreView> findHotPostScores(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("startDate") LocalDate startDate,
            @Param("today") LocalDate today,
            @Param("likeWeight") int likeWeight,
            @Param("commentWeight") int commentWeight,
            @Param("visitorWeight") int visitorWeight,
            @Param("decayRate") double decayRate,
            @Param("minReactions") int minReactions,
            @Param("excludedBoardTypesEmpty") boolean excludedBoardTypesEmpty,
            @Param("excludedBoardTypes") List<String> excludedBoardTypes,
            @Param("limit") int limit
    );

    /**
     * 좋아요 수를 단일 DB update로 증가시킨다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId AND p.deletedAt IS NULL")
    void incrementLikeCount(@Param("postId") Long postId);

    /**
     * 조회수를 단일 DB update로 증가시킨다.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Post p
            SET p.viewCount = p.viewCount + 1
            WHERE p.id = :postId
              AND p.deletedAt IS NULL
            """)
    void incrementViewCount(@Param("postId") Long postId);

    /**
     * 좋아요 수를 단일 DB update로 감소시킨다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.deletedAt IS NULL AND p.likeCount > 0")
    void decrementLikeCount(@Param("postId") Long postId);

    /**
     * 키워드로 제목 또는 본문을 검색한다. boardType이 null이면 전체 게시판을 대상으로 한다.
     */
    @Query("""
            SELECT p FROM Post p
            JOIN FETCH p.member
            WHERE p.deletedAt IS NULL
              AND p.hiddenByAdmin = false
              AND (:boardType IS NULL OR p.boardType = :boardType)
              AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)
            ORDER BY p.id DESC
            """)
    List<Post> searchByKeyword(
            @Param("keyword") String keyword,
            @Param("boardType") BoardType boardType,
            Pageable pageable
    );

    interface HotPostScoreView {
        Long getPostId();
        double getScore();
    }
}
