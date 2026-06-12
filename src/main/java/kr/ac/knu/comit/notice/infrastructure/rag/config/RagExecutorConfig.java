package kr.ac.knu.comit.notice.infrastructure.rag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class RagExecutorConfig {

    @Bean
    public ExecutorService ragVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
