package kr.ac.knu.comit.post.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.time.LocalDateTime;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.PostErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Post")
class PostTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("제목이 30자를 초과하면 INVALID_TITLE 예외를 던진다")
        void throwsWhenTitleExceedsMaxLength() {
            Member author = author();

            assertThatThrownBy(() -> Post.create(
                    author,
                    BoardType.QNA,
                    "가".repeat(PostConstraints.TITLE_MAX_LENGTH + 1),
                    "본문",
                    List.of(),
                    List.of()
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(PostErrorCode.INVALID_TITLE);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("내용이 500자를 초과하면 INVALID_CONTENT 예외를 던진다")
        void throwsWhenContentExceedsMaxLength() {
            Member author = author();
            Post post = Post.create(author, BoardType.QNA, "제목", "본문", List.of(), List.of());

            assertThatThrownBy(() -> post.update(
                    "수정 제목",
                    "나".repeat(PostConstraints.CONTENT_MAX_LENGTH + 1),
                    List.of(),
                    List.of()
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(PostErrorCode.INVALID_CONTENT);
        }
    }

    private Member author() {
        return Member.create(
                "sso-1",
                "테스트유저",
                "010-0000-0000",
                "writer",
                "20230001",
                null,
                null,
                LocalDateTime.now()
        );
    }
}
