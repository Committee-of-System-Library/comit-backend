package kr.ac.knu.comit.post.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long>, PostLikeRepositoryCustom {

    /**
     * 중복 예외를 던지지 않고 좋아요 레코드 생성을 시도한다.
     *
     * @return 좋아요가 새로 생성되면 {@code 1}, 이미 존재하면 {@code 0}
     * @implNote 중복 키 예외에 의존하면 트랜잭션이 롤백 상태가 되므로,
     * 현재 구현은 {@code INSERT IGNORE}로 결과값만 판단한다.
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "INSERT IGNORE INTO post_like (post_id, member_id, created_at) VALUES (:postId, :memberId, NOW())",
            nativeQuery = true
    )
    int insertIgnore(@Param("postId") Long postId, @Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostLike pl WHERE pl.postId = :postId AND pl.memberId = :memberId")
    void deleteByPostIdAndMemberId(@Param("postId") Long postId, @Param("memberId") Long memberId);

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
}
