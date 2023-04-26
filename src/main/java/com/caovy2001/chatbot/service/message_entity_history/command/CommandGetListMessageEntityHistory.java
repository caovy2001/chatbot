package com.caovy2001.chatbot.service.message_entity_history.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListMessageEntityHistory {
    private int page = 0;
    private int size = 0;
    private String userId;
    private String sessionId;
    private List<String> sessionIds;
    private String entityTypeId;
    private List<String> returnFields;
    @Builder.Default
    private Boolean hasEntityType = false;
}
