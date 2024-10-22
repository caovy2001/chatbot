package com.caovy2001.chatbot.service.intent.command;

import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandIntentAdd extends CommandAddBase {
    private String code;
//    private String userId;
    private String name;
}
