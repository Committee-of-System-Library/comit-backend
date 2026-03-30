package kr.ac.knu.comit.global.config;

import kr.ac.knu.comit.global.auth.MemberArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties(WebCorsProperties.class)
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MemberArgumentResolver memberArgumentResolver;
    private final WebCorsProperties webCorsProperties;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(memberArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (webCorsProperties.getAllowedOrigins().isEmpty()) {
            return;
        }

        registry.addMapping("/**")
                .allowedOrigins(webCorsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/api/docs", "/api/docs/index.html");
        registry.addRedirectViewController("/docs", "/docs/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/docs/**")
                .addResourceLocations(
                        "file:./docs/api/",
                        "file:/app/api-docs/"
                );
        registry.addResourceHandler("/docs/**")
                .addResourceLocations(
                        "file:./docs/api/",
                        "file:/app/api-docs/"
                );
    }
}
