package com.example.projeto2.WEB;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final WebGuardInterceptor webGuardInterceptor;

    public WebMvcConfig(WebGuardInterceptor webGuardInterceptor) {
        this.webGuardInterceptor = webGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webGuardInterceptor)
                .addPathPatterns("/web/**")
                .excludePathPatterns(
                        "/web",
                        "/web/",
                        "/web/login",
                        "/web/logout",
                        "/web/css/**",
                        "/web/js/**",
                        "/web/images/**"
                );
    }
}
