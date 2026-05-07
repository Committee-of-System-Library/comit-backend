package kr.ac.knu.comit.fixture;

import kr.ac.knu.comit.notice.domain.OfficialNotice;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public class OfficialNoticeFixture {

    public static OfficialNotice notice(Long id) {
        return notice(id, "공지사항 제목 " + id);
    }

    public static OfficialNotice notice(Long id, String title) {
        OfficialNotice notice = OfficialNotice.create(
                title,
                "공지사항 본문 내용입니다.",
                "학사지원팀",
                "https://computer.knu.ac.kr/notice/" + id,
                LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }
}
