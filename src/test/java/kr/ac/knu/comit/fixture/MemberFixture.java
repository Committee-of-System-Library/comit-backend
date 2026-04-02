package kr.ac.knu.comit.fixture;

import java.time.LocalDateTime;
import kr.ac.knu.comit.member.domain.Member;
import org.springframework.test.util.ReflectionTestUtils;

public class MemberFixture {

    public static Member member() {
        return member(1L);
    }

    public static Member member(Long id) {
        return member(id, "writer-" + id);
    }

    public static Member member(Long id, String nickname) {
        Member member = Member.create(
                "sso-" + id,
                "테스트유저",
                "010-0000-0000",
                nickname,
                "20230001",
                null,
                null,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
