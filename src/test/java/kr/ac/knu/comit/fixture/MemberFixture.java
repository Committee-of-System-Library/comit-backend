package kr.ac.knu.comit.fixture;

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
        Member member = Member.create("sso-" + id, nickname, "20230001");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
