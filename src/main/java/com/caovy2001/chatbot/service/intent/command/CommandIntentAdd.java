package com.caovy2001.chatbot.service.intent.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommandIntentAdd {
    private String code;
    private String user_id;
    private String name;
}
