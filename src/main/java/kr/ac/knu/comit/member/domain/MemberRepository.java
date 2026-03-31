package kr.ac.knu.comit.member.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findBySsoSubAndDeletedAtIsNull(String ssoSub);

    boolean existsBySsoSub(String ssoSub);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    @Query("""
            SELECT m FROM Member m
            WHERE m.deletedAt IS NULL
              AND (:status IS NULL OR m.status = :status)
            ORDER BY m.id DESC
            """)
    Page<Member> findAllActiveForAdmin(
            @Param("status") MemberStatus status,
            Pageable pageable
    );

    Optional<Member> findByIdAndDeletedAtIsNull(Long memberId);
}
