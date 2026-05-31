package kr.ac.knu.comit.post.config;

import kr.ac.knu.comit.post.domain.BoardType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 인기글 집계 정책 설정.
 *
 * <p>운영 중 정책 변경(집계 기간, 가중치, 최대 개수, 제외 게시판)을
 * 코드 수정 없이 {@code application.yml} 또는 환경변수로 반영할 수 있다.
 *
 * @see <a href="https://github.com/Committee-of-System-Library/knu-cse-comit-server/issues/49">issue #49</a>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "comit.post.hot")
public class HotPostPolicyProperties {

    /** 집계 기간 (일). 오늘 포함 최근 N일. */
    private int windowDays = 7;

    /** 좋아요 가중치. */
    private int likeWeight = 5;

    /** 댓글 가중치. */
    private int commentWeight = 3;

    /** 방문자 가중치. */
    private int visitorWeight = 2;

    /** 반환할 인기글 최대 개수. */
    private int limit = 5;

    /**
     * 시간 감쇠율. 하루가 지날수록 반응 가중치를 EXP(-decayRate * days) 배로 줄인다.
     * 0이면 감쇠 없음 (균등 집계).
     */
    private double decayRate = 0.1;

    /**
     * 인기글 후보가 되기 위한 최소 원시 반응 수 (좋아요 + 댓글 + unique 방문자 합산).
     * 기본값 1 — 반응이 전혀 없는 게시글만 제외한다.
     */
    private int minReactions = 1;

    /**
     * 인기글 집계에서 제외할 게시판 목록.
     * 기본값: NOTICE, EVENT — 운영 게시판은 커뮤니티 활동성 지표에서 제외한다.
     */
    private List<BoardType> excludedBoardTypes = List.of(BoardType.NOTICE, BoardType.EVENT);
}
