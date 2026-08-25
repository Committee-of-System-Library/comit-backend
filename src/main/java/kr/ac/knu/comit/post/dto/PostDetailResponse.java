package kr.ac.knu.comit.post.dto;

import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long id,
        BoardType boardType,
        String title,
        String content,
        String authorNickname,
        String authorProfileImageUrl,
        int likeCount,
        int viewCount,
        boolean likedByMe,
        List<String> tags,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 게시글 상세 응답을 만든다.
     *
     * @param viewCount 응답에 실을 조회수. 조회수 증가 UPDATE는 락 구간을 줄이려고 트랜잭션 마지막에
     * 실행되므로, 호출부가 {@code post.getViewCount() + 1}을 계산해 넘긴다
     * ({@code PostService#getPost} 참고).
     */
    public static PostDetailResponse of(Post post, boolean likedByMe, List<String> imageUrls, int viewCount) {
        return new PostDetailResponse(
                post.getId(),
                post.getBoardType(),
                post.getTitle(),
                post.getContent(),
                post.getMember().getDisplayNickname(),
                post.getMember().getProfileImageUrl(),
                post.getLikeCount(),
                viewCount,
                likedByMe,
                post.getTags().stream().map(t -> t.getName()).toList(),
                imageUrls,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
