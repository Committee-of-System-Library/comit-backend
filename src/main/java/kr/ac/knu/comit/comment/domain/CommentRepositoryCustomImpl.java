package kr.ac.knu.comit.comment.domain;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.ac.knu.comit.post.domain.QPost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Comment> findMyComments(Long memberId, Long cursorId, int limit) {
        QComment comment = QComment.comment;
        QPost post = QPost.post;

        BooleanBuilder where = new BooleanBuilder();
        where.and(comment.member.id.eq(memberId));
        where.and(comment.deletedAt.isNull());
        where.and(comment.hiddenByAdmin.isFalse());
        where.and(post.deletedAt.isNull());
        where.and(post.hiddenByAdmin.isFalse());
        if (cursorId != null) {
            where.and(comment.id.lt(cursorId));
        }

        return queryFactory
                .selectFrom(comment)
                .join(comment.post, post).fetchJoin()
                .where(where)
                .orderBy(comment.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public long countMyComments(Long memberId) {
        QComment comment = QComment.comment;
        QPost post = QPost.post;
        Long result = queryFactory
                .select(comment.count())
                .from(comment)
                .join(comment.post, post)
                .where(
                        comment.member.id.eq(memberId),
                        comment.deletedAt.isNull(),
                        comment.hiddenByAdmin.isFalse(),
                        post.deletedAt.isNull(),
                        post.hiddenByAdmin.isFalse()
                )
                .fetchOne();
        return result != null ? result : 0L;
    }
}
