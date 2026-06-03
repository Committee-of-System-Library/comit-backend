package kr.ac.knu.comit.notice.infrastructure.rag.config;

import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoticeRagConfig {

    @Bean
    @Qualifier("queryTransformClient")
    public ChatClient queryTransformClient(ChatClient.Builder builder, NoticeRagProperties properties) {
        return builder
                .defaultOptions(chatOptions(properties.getQueryTransformModel(), properties.getChatOpenAiTimeout()))
                .build();
    }

    @Bean
    @Qualifier("rerankClient")
    public ChatClient rerankClient(ChatClient.Builder builder, NoticeRagProperties properties) {
        return builder
                .defaultOptions(chatOptions(properties.getRerankModel(), properties.getChatOpenAiTimeout()))
                .build();
    }

    @Bean
    @Qualifier("answerNanoClient")
    public ChatClient answerNanoClient(ChatClient.Builder builder, NoticeRagProperties properties) {
        return builder
                .defaultOptions(chatOptions(properties.getAnswerNanoModel(), properties.getChatOpenAiTimeout()))
                .build();
    }

    @Bean
    @Qualifier("answerMiniClient")
    public ChatClient answerMiniClient(ChatClient.Builder builder, NoticeRagProperties properties) {
        return builder
                .defaultOptions(chatOptions(properties.getAnswerMiniModel(), properties.getChatOpenAiTimeout()))
                .build();
    }

    @Bean
    @Qualifier("answerClient")
    public ChatClient answerClient(ChatClient.Builder builder, NoticeRagProperties properties) {
        return builder
                .defaultOptions(chatOptions(properties.getAnswerModel(), properties.getChatOpenAiTimeout()))
                .build();
    }

    @Bean
    @Qualifier("summarizerClient")
    public ChatClient summarizerClient(ChatClient.Builder builder, NoticeRagProperties properties) {
        return builder
                .defaultOptions(OpenAiChatOptions.builder().model(properties.getSummarizerModel()))
                .build();
    }

    private OpenAiChatOptions.Builder chatOptions(String model, Duration timeout) {
        return OpenAiChatOptions.builder()
                .model(model)
                .timeout(timeout);
    }
}
