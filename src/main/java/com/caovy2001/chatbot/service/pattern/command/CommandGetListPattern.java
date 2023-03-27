package com.caovy2001.chatbot.service.pattern.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListPattern {
    private String userId;
    private String intentId;
    private String keyword;
    private int size = 0;
    private int page = 0;
}
