package com.caovy2001.chatbot.service.entity.command;

import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
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
public class CommandEntityAddMany extends CommandAddManyBase {
//    private String userId;
    private List<EntityEntity> entities;
}
