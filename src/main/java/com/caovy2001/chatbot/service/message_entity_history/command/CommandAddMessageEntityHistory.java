package com.caovy2001.chatbot.service.message_entity_history.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddMessageEntityHistory {
    private String userId;
    private String sessionId;
    private String entityTypeId;
    private List<String> values;
    @Builder.Default
    private boolean checkExist = true;
}
