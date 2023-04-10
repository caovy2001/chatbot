package com.caovy2001.chatbot.service.pattern.command;

import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandPatternUpdate {
    private String id;
    private String userId;
    private String content;
    private String intentId;
    private List<CommandAddEntity> entities;
}
