package kr.ac.knu.comit.member.service;

import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.comment.domain.CommentRepository;
import kr.ac.knu.comit.comment.service.CommentQueryService;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.member.dto.MyActivitySummaryResponse;
import kr.ac.knu.comit.member.dto.MyCommentCursorPageResponse;
import kr.ac.knu.comit.member.dto.MyCommentSummary;
import kr.ac.knu.comit.member.dto.MyLikedPostCursorPageResponse;
import kr.ac.knu.comit.member.dto.MyLikedPostSummary;
import kr.ac.knu.comit.member.dto.MyPostSummary;
import kr.ac.knu.comit.post.domain.Post;
import kr.ac.knu.comit.post.domain.PostImage;
import kr.ac.knu.comit.post.domain.PostImageRepository;
import kr.ac.knu.comit.post.domain.PostLike;
import kr.ac.knu.comit.post.domain.PostLikeRepository;
import kr.ac.knu.comit.post.domain.PostRepository;
import kr.ac.knu.comit.post.dto.PostCursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberActivityService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int RECENT_ITEM_COUNT = 3;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final CommentQueryService commentQueryService;
    private final PostLikeRepository postLikeRepository;

    public PostCursorPageResponse getMyPosts(Long memberId, Long cursorId, int size) {
        if (size <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        int pageSize = Math.min(size, DEFAULT_PAGE_SIZE);
        List<Post> posts = postRepository.findMyPosts(memberId, cursorId, pageSize + 1);
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        return PostCursorPageResponse.of(
                posts,
                pageSize,
                commentQueryService.countActiveCommentsByPostIds(postIds),
                imageUrlsByPostId(postIds)
        );
    }

    public MyActivitySummaryResponse getActivitySummary(Long memberId) {
        long postCount    = postRepository.countMyPosts(memberId);
        long commentCount = commentRepository.countMyComments(memberId);
        long likeCount    = postLikeRepository.countMyLikes(memberId);

        List<MyPostSummary> recentPosts = postRepository
                .findMyPosts(memberId, null, RECENT_ITEM_COUNT)
                .stream().map(MyPostSummary::from).toList();

        List<MyCommentSummary> recentComments = commentRepository
                .findMyComments(memberId, null, RECENT_ITEM_COUNT)
                .stream().map(MyCommentSummary::from).toList();

        List<PostLike> recentLikes = postLikeRepository.findMyLikes(memberId, null, RECENT_ITEM_COUNT);
        List<MyLikedPostSummary> recentLikedPosts = mapLikesToSummaries(recentLikes);

        return new MyActivitySummaryResponse(postCount, commentCount, likeCount,
                recentPosts, recentComments, recentLikedPosts);
    }

    public MyCommentCursorPageResponse getMyComments(Long memberId, Long cursorId, int size) {
        if (size <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        int pageSize = Math.min(size, DEFAULT_PAGE_SIZE);
        List<Comment> comments = commentRepository.findMyComments(memberId, cursorId, pageSize + 1);
        return MyCommentCursorPageResponse.of(comments, pageSize);
    }

    public MyLikedPostCursorPageResponse getMyLikes(Long memberId, Long cursorId, int size) {
        if (size <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        int pageSize = Math.min(size, DEFAULT_PAGE_SIZE);
        List<PostLike> likes = postLikeRepository.findMyLikes(memberId, cursorId, pageSize + 1);

        List<Long> postIds = likes.stream().map(PostLike::getPostId).toList();
        Map<Long, Post> postById = postRepository.findActiveWithMemberAndTagsByIds(postIds)
                .stream().collect(Collectors.toMap(Post::getId, Function.identity()));

        return MyLikedPostCursorPageResponse.of(likes, postById, pageSize);
    }

    private List<MyLikedPostSummary> mapLikesToSummaries(List<PostLike> likes) {
        if (likes.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = likes.stream().map(PostLike::getPostId).toList();
        Map<Long, Post> postById = postRepository.findActiveWithMemberAndTagsByIds(postIds)
                .stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        return likes.stream()
                .filter(like -> postById.containsKey(like.getPostId()))
                .map(like -> MyLikedPostSummary.from(like, postById.get(like.getPostId())))
                .toList();
    }

    private Map<Long, List<String>> imageUrlsByPostId(List<Long> postIds) {
        return postImageRepository.findByPost_IdInOrderBySortOrderAsc(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        PostImage::getPostId,
                        Collectors.mapping(PostImage::getImageUrl, Collectors.toList())
                ));
    }
}
