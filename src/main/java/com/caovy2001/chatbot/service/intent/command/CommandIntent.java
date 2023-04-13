package com.caovy2001.chatbot.service.intent.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommandIntent {
    private String id;
    private String code;
    private String userId;
    private String name;
}
