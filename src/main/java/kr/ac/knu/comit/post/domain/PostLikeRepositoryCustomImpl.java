package kr.ac.knu.comit.post.domain;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostLikeRepositoryCustomImpl implements PostLikeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PostLike> findMyLikes(Long memberId, Long cursorId, int limit) {
        QPostLike postLike = QPostLike.postLike;
        QPost post = QPost.post;

        BooleanBuilder where = new BooleanBuilder();
        where.and(postLike.memberId.eq(memberId));
        if (cursorId != null) {
            where.and(postLike.id.lt(cursorId));
        }

        return queryFactory
                .selectFrom(postLike)
                .join(post).on(
                        post.id.eq(postLike.postId),
                        post.deletedAt.isNull(),
                        post.hiddenByAdmin.isFalse()
                )
                .where(where)
                .orderBy(postLike.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public long countMyLikes(Long memberId) {
        QPostLike postLike = QPostLike.postLike;
        QPost post = QPost.post;

        Long result = queryFactory
                .select(postLike.count())
                .from(postLike)
                .join(post).on(
                        post.id.eq(postLike.postId),
                        post.deletedAt.isNull(),
                        post.hiddenByAdmin.isFalse()
                )
                .where(postLike.memberId.eq(memberId))
                .fetchOne();
        return result != null ? result : 0L;
    }
}
