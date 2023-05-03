package com.caovy2001.chatbot.service.intent.command;

import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CommandIntentAddMany extends CommandAddManyBase {
//    private String userId;
    private List<IntentEntity> intents;
    @Builder.Default
    private Boolean returnSameCodeIntent = false;
}
