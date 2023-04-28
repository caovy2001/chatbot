package com.caovy2001.chatbot.service.intent.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandIntentAdd {
    private String code;
    private String userId;
    private String name;
}
