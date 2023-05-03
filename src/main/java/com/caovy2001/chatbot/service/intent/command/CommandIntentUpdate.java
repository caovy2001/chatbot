package com.caovy2001.chatbot.service.intent.command;

import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandIntentUpdate extends CommandUpdateBase {
    private String id;
    private String code;
    private String name;
}
