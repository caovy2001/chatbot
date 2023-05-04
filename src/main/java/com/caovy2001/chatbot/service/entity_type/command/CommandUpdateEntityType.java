package com.caovy2001.chatbot.service.entity_type.command;

import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandUpdateEntityType extends CommandUpdateBase {
    private String id;
//    private String userId;
    private String name;
}
