package com.zoominfo.karan_take_home.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ConfigController {
    
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;
    
    @Value("${spring.servlet.multipart.max-request-size}")
    private String maxRequestSize;
    
    @Value("${server.tomcat.max-http-post-size}")
    private String maxHttpPostSize;
    
    @Value("${server.tomcat.max-swallow-size}")
    private String maxSwallowSize;
    
    @GetMapping("/config")
    public Map<String, String> getConfig() {
        return Map.of(
            "max-file-size", maxFileSize,
            "max-request-size", maxRequestSize,
            "max-http-post-size", maxHttpPostSize,
            "max-swallow-size", maxSwallowSize
        );
    }
}
