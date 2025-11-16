package com.zoominfo.karan_take_home.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.zoominfo.karan_take_home.clients.FasterWhisperClient;

@Configuration
public class FasterWhisperClientConfig {
    
    @Bean
    public FasterWhisperClient fasterWhisperClient() {
        WebClient webClient = WebClient.builder()
            .baseUrl("http://faster-whisper-server:8000")
            .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder()
            .exchangeAdapter(WebClientAdapter.create(webClient))
            .build();
        return factory.createClient(FasterWhisperClient.class);
    }
}