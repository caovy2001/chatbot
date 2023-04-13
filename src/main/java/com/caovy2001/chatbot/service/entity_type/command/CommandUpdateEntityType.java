package com.caovy2001.chatbot.service.entity_type.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandUpdateEntityType {
    private String id;
    private String userId;
    private String name;
}
