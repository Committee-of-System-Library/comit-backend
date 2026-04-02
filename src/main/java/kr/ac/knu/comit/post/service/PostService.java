package kr.ac.knu.comit.post.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.PostErrorCode;
import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.domain.*;
import kr.ac.knu.comit.post.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int HOT_POST_WINDOW_DAYS = 7;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostDailyVisitorRepository postDailyVisitorRepository;
    private final MemberService memberService;
    private final CommentQueryService commentQueryService;

    /**
     * cursor 기반으로 게시글 목록을 조회한다.
     *
     * @apiNote {@code cursor}가 {@code null}이면 첫 페이지를 조회한다.
     * @implNote 리포지토리는 작성자 정보를 함께 가져오고, 댓글 수는 별도로 채워
     * 서비스 메서드가 위에서 아래로 읽히는 흐름을 유지한다.
     */
    public PostCursorPageResponse getPosts(BoardType boardType, Long cursorId, int size) {
        if (size <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        int pageSize = Math.min(size, DEFAULT_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(0, pageSize + 1);

        List<Post> posts = (cursorId == null)
                ? postRepository.findFirstPage(boardType, pageable)
                : postRepository.findByCursor(boardType, cursorId, pageable);

        return PostCursorPageResponse.of(
                posts,
                pageSize,
                commentQueryService.countActiveCommentsByPostIds(posts.stream().map(Post::getId).toList())
        );
    }

    public HotPostListResponse getHotPosts() {
        LocalDate startDate = LocalDate.now(KST).minusDays(HOT_POST_WINDOW_DAYS - 1L);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        List<Long> orderedPostIds = postRepository.findHotPostScores(startDateTime, startDate).stream()
                .map(PostRepository.HotPostScoreView::getPostId)
                .toList();

        if (orderedPostIds.isEmpty()) {
            return HotPostListResponse.empty();
        }

        List<Post> posts = postRepository.findActiveWithMemberAndTagsByIds(orderedPostIds);
        List<Long> visiblePostIds = posts.stream().map(Post::getId).toList();

        return HotPostListResponse.of(
                posts,
                orderedPostIds,
                commentQueryService.countActiveCommentsByPostIds(visiblePostIds)
        );
    }

    @Transactional
    public PostDetailResponse getPost(Long postId, Long memberId) {
        findPostOrThrow(postId);
        postRepository.incrementViewCount(postId);
        Post post = findPostOrThrow(postId);
        recordDailyVisitor(postId, memberId);
        boolean likedByMe = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        return PostDetailResponse.of(post, likedByMe);
    }

    @Transactional
    public Long createPost(Long memberId, CreatePostRequest request) {
        Member author = memberService.findMemberOrThrow(memberId);
        Post post = Post.create(author, request.boardType(), request.title(),
                request.content(), request.tags(), request.imageUrls());
        return postRepository.save(post).getId();
    }

    @Transactional
    public void updatePost(Long memberId, Long postId, UpdatePostRequest request) {
        Post post = findPostOrThrow(postId);
        checkOwnership(post, memberId);
        post.update(request.title(), request.content(), request.tags());
    }

    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Post post = findPostOrThrow(postId);
        checkOwnership(post, memberId);
        deletePost(post);
    }

    @Transactional
    public void deletePost(Long memberId, Long postId, boolean admin) {
        Post post = findPostOrThrow(postId);
        if (!admin) {
            checkOwnership(post, memberId);
        }
        deletePost(post);
    }

    /**
     * 요청한 사용자의 게시글 좋아요 상태를 토글한다.
     *
     * @implNote 현재 구현은 DB 유니크 키 {@code (post_id, member_id)}와
     * 원자적 카운터 업데이트에 기대어 메모리 기반 동기화를 피한다.
     */
    @Transactional
    public LikeToggleResponse toggleLike(Long memberId, Long postId) {
        findPostOrThrow(postId);

        int inserted = postLikeRepository.insertIgnore(postId, memberId);

        if (inserted == 1) {
            postRepository.incrementLikeCount(postId);
            return LikeToggleResponse.likedState();
        } else {
            postLikeRepository.deleteByPostIdAndMemberId(postId, memberId);
            postRepository.decrementLikeCount(postId);
            return LikeToggleResponse.unlikedState();
        }
    }

    public Post getActivePostOrThrow(Long postId) {
        return findPostOrThrow(postId);
    }

    private void recordDailyVisitor(Long postId, Long memberId) {
        postDailyVisitorRepository.insertIgnore(postId, memberId, LocalDate.now(KST));
    }

    private Post findPostOrThrow(Long postId) {
        return postRepository.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
    }

    private void checkOwnership(Post post, Long memberId) {
        if (!post.isWrittenBy(memberId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void deletePost(Post post) {
        post.delete();
    }
}
