package kr.ac.knu.comit.notice.infra;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class NoticeSummarizer {

    private final ChatClient chatClient;

    public NoticeSummarizer(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String summarize(String title, String content) {
        return chatClient.prompt()
                .system("""
                        당신은 대학교 공지사항을 간결하게 요약하는 도우미입니다.
                        핵심 내용만 2~3문장으로 요약하세요. 불필요한 인사말이나 부연 설명은 생략합니다.

                        예시 1)
                        제목: 2025학년도 2학기 수강신청 일정 안내
                        본문: 2025학년도 2학기 수강신청은 7월 14일(월) 09:00부터 7월 16일(수) 18:00까지 진행됩니다. 수강신청은 포털시스템(http://portal.knu.ac.kr)에서 가능하며, 수강 정정 기간은 8월 25일(월)부터 8월 29일(금)까지입니다. 장바구니 신청은 7월 7일(월)부터 가능합니다.
                        요약: 2025학년도 2학기 수강신청은 7월 14일~16일, 포털시스템에서 진행됩니다. 장바구니 신청은 7월 7일부터 가능하며, 수강 정정은 8월 25일~29일입니다.

                        예시 2)
                        제목: 졸업논문 및 졸업시험 신청 안내
                        본문: 2025학년도 2학기 졸업논문 및 졸업시험 신청을 아래와 같이 안내합니다. 신청 기간은 9월 1일(월)부터 9월 5일(금) 17:00까지이며, 신청 방법은 학과 사무실 방문 또는 이메일(cs@knu.ac.kr) 제출입니다. 졸업 예정자는 반드시 기간 내 신청하시기 바랍니다.
                        요약: 2025학년도 2학기 졸업논문·졸업시험 신청은 9월 1일~5일까지 학과 사무실 방문 또는 이메일로 접수합니다. 졸업 예정자는 기간 내 필수 신청해야 합니다.
                        """)
                .user(u -> u
                        .text("제목: {title}\n본문: {content}\n요약:")
                        .param("title", title)
                        .param("content", content)
                )
                .call()
                .content();
    }
}
