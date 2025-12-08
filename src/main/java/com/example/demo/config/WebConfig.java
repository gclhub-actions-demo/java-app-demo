package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for resource handlers and static content.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String CONTROLLER_PACKAGE = "com.example.demo.controller";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Add /api prefix to all controller mappings
        configurer.addPathPrefix("/api", clazz -> clazz.getPackageName().startsWith(CONTROLLER_PACKAGE));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configure static resource handlers with specific paths
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/public/");
        
        // Configure webjars for UI libraries
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
