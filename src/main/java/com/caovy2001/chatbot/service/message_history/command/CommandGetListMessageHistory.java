package com.caovy2001.chatbot.service.message_history.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListMessageHistory {
    private int page;
    private int size;
    private String userId;
    private String scriptId;
    private String sessionId;
}
