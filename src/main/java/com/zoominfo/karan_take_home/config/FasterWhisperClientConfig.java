package com.zoominfo.karan_take_home.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.zoominfo.karan_take_home.clients.FasterWhisperClient;
import com.zoominfo.karan_take_home.interceptors.FasterWhisperClientInterceptor;

@Configuration
public class FasterWhisperClientConfig {
    
    @Value("${faster.whisper.url}")
    private String fasterWhisperUrl;
    
    @Bean
    public FasterWhisperClient fasterWhisperClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
            })
            .build();
        
        WebClient webClient = WebClient.builder()
            .baseUrl(fasterWhisperUrl)
            .exchangeStrategies(strategies)
            .filter(FasterWhisperClientInterceptor.all())
            .build();
        
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder()
            .exchangeAdapter(WebClientAdapter.create(webClient))
            .build();
        return factory.createClient(FasterWhisperClient.class);
    }
}