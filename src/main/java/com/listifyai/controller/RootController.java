package com.listifyai.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    private final String frontendUrl;

    public RootController(@Value("${frontend.url:${FRONTEND_URL:http://127.0.0.1:5173}}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/")
    public ResponseEntity<Void> redirectToFrontend() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, frontendUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
