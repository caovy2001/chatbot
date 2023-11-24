package com.caovy2001.chatbot.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class WelcomeAPI {
    @GetMapping("/")
    public String welcome() {
        return "Welcome to Chatbot service";
    }
}
