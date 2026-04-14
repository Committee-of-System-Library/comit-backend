package kr.ac.knu.comit.post.domain;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Post> findMyPosts(Long memberId, Long cursorId, int limit) {
        QPost post = QPost.post;

        BooleanBuilder where = new BooleanBuilder();
        where.and(post.member.id.eq(memberId));
        where.and(post.deletedAt.isNull());
        where.and(post.hiddenByAdmin.isFalse());
        if (cursorId != null) {
            where.and(post.id.lt(cursorId));
        }

        return queryFactory
                .selectFrom(post)
                .join(post.member).fetchJoin()
                .where(where)
                .orderBy(post.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public long countMyPosts(Long memberId) {
        QPost post = QPost.post;
        Long result = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        post.member.id.eq(memberId),
                        post.deletedAt.isNull(),
                        post.hiddenByAdmin.isFalse()
                )
                .fetchOne();
        return result != null ? result : 0L;
    }

    @Override
    public List<Post> searchByKeywordWithCursor(String keyword, BoardType boardType, Long cursorId, int limit) {
        QPost post = QPost.post;

        BooleanBuilder where = new BooleanBuilder();
        where.and(post.deletedAt.isNull());
        where.and(post.hiddenByAdmin.isFalse());
        where.and(post.title.contains(keyword).or(post.content.contains(keyword)));
        if (boardType != null) {
            where.and(post.boardType.eq(boardType));
        }
        if (cursorId != null) {
            where.and(post.id.lt(cursorId));
        }

        return queryFactory
                .selectFrom(post)
                .join(post.member).fetchJoin()
                .where(where)
                .orderBy(post.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByKeyword(String keyword, BoardType boardType) {
        QPost post = QPost.post;

        BooleanBuilder where = new BooleanBuilder();
        where.and(post.deletedAt.isNull());
        where.and(post.hiddenByAdmin.isFalse());
        where.and(post.title.contains(keyword).or(post.content.contains(keyword)));
        if (boardType != null) {
            where.and(post.boardType.eq(boardType));
        }

        Long result = queryFactory
                .select(post.count())
                .from(post)
                .where(where)
                .fetchOne();
        return result != null ? result : 0L;
    }
}
