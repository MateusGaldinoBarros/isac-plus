package com.dev.isac_plus.restClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

@Configuration 
public class EvolutionConfig {
    
    @Bean
    public RestClient evolutionRestClient(
        @Value("${evolution.api.url}") String baseUrl,
        @Value("${evolution.api.key}") String apiKey) {
        
            return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("apikey", apiKey)
            .build();
    }
}