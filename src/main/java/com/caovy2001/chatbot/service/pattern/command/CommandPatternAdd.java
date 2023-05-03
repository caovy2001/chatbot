package com.caovy2001.chatbot.service.pattern.command;

import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
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
public class CommandPatternAdd extends CommandAddBase {
//    private String userId;
    private String content;
    private String intentId;
    private List<EntityEntity> entities;
    private CommandEntityAddMany commandEntityAddMany;
}
