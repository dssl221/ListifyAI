package com.listifyai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ListifyAiApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ListifyAiApplication.class);
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("spring.profiles.default", "dev");
        app.setDefaultProperties(defaultProps);
        app.run(args);
    }
}
