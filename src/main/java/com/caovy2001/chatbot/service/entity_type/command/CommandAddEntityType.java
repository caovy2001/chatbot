package com.caovy2001.chatbot.service.entity_type.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommandAddEntityType {
    private String userId;
    private String name;

    private String uuid;
}
