package kr.ac.knu.comit.post.service;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.PostErrorCode;
import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.service.MemberService;
import kr.ac.knu.comit.post.config.HotPostPolicyProperties;
import kr.ac.knu.comit.post.domain.*;
import kr.ac.knu.comit.post.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int SEARCH_RESULT_LIMIT = 5;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostDailyVisitorRepository postDailyVisitorRepository;
    private final MemberService memberService;
    private final CommentQueryService commentQueryService;
    private final ContentPreviewGenerator contentPreviewGenerator;
    private final HotPostPolicyProperties hotPostPolicy;

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

        List<Long> postIds = posts.stream().map(Post::getId).toList();

        return PostCursorPageResponse.of(
                posts,
                pageSize,
                commentQueryService.countActiveCommentsByPostIds(postIds),
                imageUrlsByPostId(postIds),
                contentPreviewGenerator::generate
        );
    }

    @Cacheable("hotPosts")
    public HotPostListResponse getHotPosts() {
        LocalDate startDate = LocalDate.now(KST).minusDays(hotPostPolicy.getWindowDays() - 1L);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        List<String> excludedBoardTypeNames = hotPostPolicy.getExcludedBoardTypes().stream()
                .map(Enum::name)
                .toList();

        List<Long> orderedPostIds = postRepository.findHotPostScores(
                startDateTime,
                startDate,
                hotPostPolicy.getLikeWeight(),
                hotPostPolicy.getCommentWeight(),
                hotPostPolicy.getVisitorWeight(),
                hotPostPolicy.getDecayRate(),
                hotPostPolicy.getMinReactions(),
                excludedBoardTypeNames.isEmpty(),
                excludedBoardTypeNames,
                hotPostPolicy.getLimit()
        ).stream()
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

    public List<PostSearchResult> searchPosts(String keyword, BoardType boardType) {
        List<Post> posts = postRepository.searchByKeyword(
                keyword, boardType, PageRequest.of(0, SEARCH_RESULT_LIMIT)
        );
        return posts.stream()
                .map(PostSearchResult::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PostDetailResponse getPost(Long postId, Long memberId) {
        findPostOrThrow(postId);
        postRepository.incrementViewCount(postId);
        Post post = findPostOrThrow(postId);
        recordDailyVisitor(postId, memberId);
        boolean likedByMe = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        List<String> imageUrls = postImageRepository.findByPost_IdOrderBySortOrderAsc(postId)
                .stream().map(PostImage::getImageUrl).toList();
        return PostDetailResponse.of(post, likedByMe, imageUrls);
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
        post.update(request.title(), request.content(), request.tags(), request.imageUrls());
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

    /**
     * 회원 삭제 시 게시글 좋아요와 방문 기록을 정리한다.
     *
     * @implNote 좋아요 row를 먼저 조회해 likeCount를 맞춘 뒤, 좋아요/방문 기록을 제거한다.
     */
    @Transactional
    public void removeMemberInteractions(Long memberId) {
        List<Long> likedPostIds = postLikeRepository.findPostIdsByMemberId(memberId);
        likedPostIds.forEach(postRepository::decrementLikeCount);
        postLikeRepository.deleteAllByMemberId(memberId);
        postDailyVisitorRepository.deleteAllByMemberId(memberId);
    }

    private Map<Long, List<String>> imageUrlsByPostId(List<Long> postIds) {
        return postImageRepository.findByPost_IdInOrderBySortOrderAsc(postIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        PostImage::getPostId,
                        java.util.stream.Collectors.mapping(PostImage::getImageUrl, java.util.stream.Collectors.toList())
                ));
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
