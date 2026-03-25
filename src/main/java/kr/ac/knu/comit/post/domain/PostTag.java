package kr.ac.knu.comit.post.domain;

import jakarta.persistence.*;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.PostErrorCode;

@Entity
@Table(name = "post_tag")
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 20)
    private String name;

    protected PostTag() {
    }

    static PostTag of(Post post, String name) {
        validateName(name);
        PostTag tag = new PostTag();
        tag.post = post;
        tag.name = name.strip();
        return tag;
    }

    static void validateName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > PostConstraints.TAG_NAME_MAX_LENGTH) {
            throw new BusinessException(PostErrorCode.INVALID_TAG);
        }
    }

    public String getName() {
        return name;
    }
}
