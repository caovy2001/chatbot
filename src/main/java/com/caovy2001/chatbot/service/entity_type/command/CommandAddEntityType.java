package com.caovy2001.chatbot.service.entity_type.command;

import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CommandAddEntityType extends CommandAddBase {
//    private String userId;
    private String name;
    private String uuid;
}
