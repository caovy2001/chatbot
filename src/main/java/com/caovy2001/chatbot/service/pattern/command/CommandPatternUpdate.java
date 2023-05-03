package com.caovy2001.chatbot.service.pattern.command;

import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CommandPatternUpdate extends CommandUpdateBase {
    private String id;
//    private String userId;
    private String content;
    private String intentId;
    private List<CommandAddEntity> entities;
}
