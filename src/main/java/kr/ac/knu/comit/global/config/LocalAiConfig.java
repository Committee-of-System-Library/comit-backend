package kr.ac.knu.comit.global.config;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalAiConfig {

    private static final int EMBEDDING_DIMENSIONS = 1536;

    @Bean
    @Primary
    public ChatModel chatModel() {
        return prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(""))));
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new AbstractEmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = request.getInstructions().stream()
                        .map(text -> new Embedding(new float[EMBEDDING_DIMENSIONS], 0))
                        .toList();
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                return new float[EMBEDDING_DIMENSIONS];
            }

            @Override
            public int dimensions() {
                return EMBEDDING_DIMENSIONS;
            }
        };
    }
}
