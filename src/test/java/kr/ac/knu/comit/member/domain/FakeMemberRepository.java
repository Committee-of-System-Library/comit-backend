package kr.ac.knu.comit.member.domain;

import kr.ac.knu.comit.support.FakeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * MemberRepository 인메모리 구현체. 단순 필드 매칭 쿼리만 구현하고, 페이징 쿼리는 미지원.
 */
public class FakeMemberRepository extends FakeRepository<Member> implements MemberRepository {

    @Override
    protected Long getId(Member member) {
        return member.getId();
    }

    @Override
    public Optional<Member> findBySsoSubAndDeletedAtIsNull(String ssoSub) {
        return store.values().stream()
                .filter(m -> m.getSsoSub().equals(ssoSub) && !m.isDeleted())
                .findFirst();
    }

    @Override
    public boolean existsBySsoSub(String ssoSub) {
        return store.values().stream()
                .anyMatch(m -> m.getSsoSub().equals(ssoSub));
    }

    @Override
    public boolean existsBySsoSubAndDeletedAtIsNotNull(String ssoSub) {
        return store.values().stream()
                .anyMatch(m -> m.getSsoSub().equals(ssoSub) && m.isDeleted());
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return store.values().stream()
                .anyMatch(m -> m.getNickname().equals(nickname));
    }

    @Override
    public boolean existsByNicknameAndIdNot(String nickname, Long id) {
        return store.values().stream()
                .anyMatch(m -> m.getNickname().equals(nickname) && !m.getId().equals(id));
    }

    @Override
    public Page<Member> findAllActiveForAdmin(MemberStatus status, Pageable pageable) {
        throw new UnsupportedOperationException("Fake does not support findAllActiveForAdmin");
    }

    @Override
    public Optional<Member> findByIdAndDeletedAtIsNull(Long memberId) {
        return store.values().stream()
                .filter(m -> m.getId().equals(memberId) && !m.isDeleted())
                .findFirst();
    }

    @Override
    public Optional<Member> findByNicknameAndDeletedAtIsNull(String nickname) {
        return store.values().stream()
                .filter(m -> m.getNickname().equals(nickname) && !m.isDeleted())
                .findFirst();
    }
}
