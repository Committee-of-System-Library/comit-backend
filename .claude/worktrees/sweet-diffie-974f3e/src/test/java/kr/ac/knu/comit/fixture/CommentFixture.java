package kr.ac.knu.comit.fixture;

import kr.ac.knu.comit.comment.domain.Comment;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.post.domain.Post;
import org.springframework.test.util.ReflectionTestUtils;

public class CommentFixture {

    public static Comment topLevelComment(Long id, Post post, Member author, String content, int likeCount) {
        Comment comment = Comment.create(post, author, content);
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "likeCount", likeCount);
        return comment;
    }

    public static Comment replyComment(Long id, Post post, Comment parent, Member author, String content, int likeCount) {
        Comment comment = Comment.reply(post, parent, author, content);
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "likeCount", likeCount);
        return comment;
    }
}
