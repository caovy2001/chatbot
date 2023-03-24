package com.caovy2001.chatbot.service.entity.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddEntity {
    private String userId;
    private String value;
    private String patternId;
    private String entityTypeId;
    private int startPosition;
    private int endPosition;
}
