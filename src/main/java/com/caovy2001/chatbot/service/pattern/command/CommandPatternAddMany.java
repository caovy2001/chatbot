package com.caovy2001.chatbot.service.pattern.command;

import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandPatternAddMany extends CommandAddManyBase {
//    private String userId;
    private List<PatternEntity> patterns;
    private CommandEntityAddMany commandEntityAddMany;
}
