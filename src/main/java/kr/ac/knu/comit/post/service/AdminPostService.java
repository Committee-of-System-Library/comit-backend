package kr.ac.knu.comit.post.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.PostErrorCode;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostRepository;
import kr.ac.knu.comit.post.dto.AdminPostPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;

    public AdminPostPageResponse getPosts(BoardType boardType, Pageable pageable) {
        Page<Post> postPage = postRepository.findAllActiveForAdmin(boardType, pageable);
        return AdminPostPageResponse.from(postPage);
    }

    @Transactional
    public void hidePost(Long postId) {
        Post post = findPostOrThrow(postId);
        post.hideByAdmin();
    }

    @Transactional
    public void restorePost(Long postId) {
        Post post = findPostOrThrow(postId);
        post.restoreByAdmin();
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = findPostOrThrow(postId);
        post.delete();
    }

    private Post findPostOrThrow(Long postId) {
        return postRepository.findActiveByIdForAdmin(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
    }
}
