package com.socialnetwork.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
      // Supports dev setups like http://localhost:5173 and even file:// (Origin: null)
      .allowedOriginPatterns("*")
      .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .exposedHeaders("*")
      .allowCredentials(false)
      .maxAge(3600);
  }
}