package kr.ac.knu.comit.post.domain;

/**
 * 게시글 도메인 전반에서 공유하는 입력 길이 정책.
 */
public final class PostConstraints {

    public static final int TITLE_MAX_LENGTH = 30;
    public static final int CONTENT_MAX_LENGTH = 500;
    public static final int TAG_MAX_COUNT = 5;
    public static final int TAG_NAME_MAX_LENGTH = 20;

    private PostConstraints() {
    }
}
