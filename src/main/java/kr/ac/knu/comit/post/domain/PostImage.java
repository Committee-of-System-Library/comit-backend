package kr.ac.knu.comit.post.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "post_image")
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int sortOrder;

    protected PostImage() {
    }

    static PostImage of(Post post, String imageUrl, int sortOrder) {
        PostImage postImage = new PostImage();
        postImage.post = post;
        postImage.imageUrl = imageUrl;
        postImage.sortOrder = sortOrder;
        return postImage;
    }

    public Long getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
