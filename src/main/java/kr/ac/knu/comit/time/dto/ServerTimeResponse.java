package kr.ac.knu.comit.time.dto;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 서버 현재 시각 응답.
 *
 * @param serverTime ISO-8601 오프셋 형식 문자열(예: {@code 2026-06-03T21:28:03.123+09:00}). 사람이 읽기/로깅용.
 * @param epoch      Unix epoch 기준 밀리초(ms). JS {@code Date.now()}와 직접 비교 가능.
 */
public record ServerTimeResponse(
        String serverTime,
        long epoch
) {

    /** 현재 시각으로 응답을 생성한다. serverTime·epoch 는 동일한 순간에서 파생된다. */
    public static ServerTimeResponse now() {
        OffsetDateTime now = OffsetDateTime.now();
        return new ServerTimeResponse(
                now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                now.toInstant().toEpochMilli()
        );
    }
}
