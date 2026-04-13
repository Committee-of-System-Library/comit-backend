package kr.ac.knu.comit.post.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.PostErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.domain.BoardType;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostRepository;
import kr.ac.knu.comit.post.dto.AdminCreatePostRequest;
import kr.ac.knu.comit.post.dto.AdminCreatePostResponse;
import kr.ac.knu.comit.post.dto.AdminPostDetailResponse;
import kr.ac.knu.comit.post.dto.AdminPostPageResponse;
import kr.ac.knu.comit.post.dto.AdminUpdatePostRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminPostService {

    private static final Set<BoardType> ADMIN_ONLY_BOARD_TYPES = Set.of(
            BoardType.NOTICE, BoardType.EVENT, BoardType.INFO
    );

    private final PostRepository postRepository;
    private final MemberService memberService;

    @Transactional
    public AdminCreatePostResponse createPost(Long adminMemberId, AdminCreatePostRequest request) {
        validateAdminBoardType(request.boardType());
        Member author = memberService.findMemberOrThrow(adminMemberId);
        Post post = Post.create(author, request.boardType(), request.title(),
                request.content(), request.tags(), request.imageUrls());
        return new AdminCreatePostResponse(postRepository.save(post).getId());
    }

    public AdminPostPageResponse getPosts(BoardType boardType, Pageable pageable) {
        Page<Post> postPage = postRepository.findAllActiveForAdmin(boardType, pageable);
        return AdminPostPageResponse.from(postPage);
    }

    public AdminPostDetailResponse getPost(Long postId) {
        Post post = findPostOrThrow(postId);
        validateAdminBoardType(post.getBoardType());
        return AdminPostDetailResponse.from(post);
    }

    @Transactional
    public void updatePost(Long postId, AdminUpdatePostRequest request) {
        Post post = findPostOrThrow(postId);
        validateAdminBoardType(post.getBoardType());
        validateAdminBoardType(request.boardType());
        post.updateByAdmin(
                request.boardType(),
                request.title(),
                request.content(),
                request.tags(),
                request.imageUrls()
        );
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

    private void validateAdminBoardType(BoardType boardType) {
        if (!ADMIN_ONLY_BOARD_TYPES.contains(boardType)) {
            throw new BusinessException(PostErrorCode.FORBIDDEN_BOARD_TYPE);
        }
    }
}
