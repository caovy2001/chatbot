package com.caovy2001.chatbot.service.entity_type.command;

import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandEntityTypeAddMany extends CommandAddManyBase {
//    private String userId;
    private List<EntityTypeEntity> entityTypes;
}
