package com.caovy2001.chatbot.service.intent.command;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CommandIntentSuggestPattern {
    private String userId;
    private String intentId;
    private Integer numOfPatterns;
    private String examplePattern;
}
