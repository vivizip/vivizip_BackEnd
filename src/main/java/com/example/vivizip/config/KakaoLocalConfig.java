package com.example.vivizip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KakaoLocalConfig {

    @Value("${kakao.local.base-url}")
    private String baseUrl;

    @Value("${kakao.local.api-key}")
    private String apiKey;

    @Bean
    public RestClient kakaoLocalRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "KakaoAK " + apiKey)  // Bearer 아님!
                .build();
    }
}
