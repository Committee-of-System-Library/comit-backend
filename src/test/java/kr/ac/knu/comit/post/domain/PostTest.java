package kr.ac.knu.comit.post.domain;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.PostErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Post")
class PostTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("제목이 30자를 초과하면 INVALID_TITLE 예외를 던진다")
        void throwsWhenTitleExceedsMaxLength() {
            Member author = Member.create("sso-1", "writer", "20230001");

            assertThatThrownBy(() -> Post.create(
                    author,
                    BoardType.QNA,
                    "가".repeat(PostConstraints.TITLE_MAX_LENGTH + 1),
                    "본문",
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
            Member author = Member.create("sso-1", "writer", "20230001");
            Post post = Post.create(author, BoardType.QNA, "제목", "본문", List.of());

            assertThatThrownBy(() -> post.update(
                    "수정 제목",
                    "나".repeat(PostConstraints.CONTENT_MAX_LENGTH + 1),
                    List.of()
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(PostErrorCode.INVALID_CONTENT);
        }
    }
}
