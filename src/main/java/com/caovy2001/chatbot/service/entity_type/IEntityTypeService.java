package com.caovy2001.chatbot.service.entity_type;

import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.entity_type.command.CommandEntityTypeAddMany;

import java.util.List;

public interface IEntityTypeService extends IBaseService {
    List<EntityTypeEntity> addMany(CommandEntityTypeAddMany command);
}
