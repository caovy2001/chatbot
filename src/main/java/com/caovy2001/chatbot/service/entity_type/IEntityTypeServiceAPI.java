package com.caovy2001.chatbot.service.entity_type;

import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.entity_type.command.CommandAddEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandUpdateEntityType;

public interface IEntityTypeServiceAPI extends IBaseService {
    EntityTypeEntity add(CommandAddEntityType command) throws Exception;
    EntityTypeEntity update(CommandUpdateEntityType command) throws Exception;

    Paginated<EntityTypeEntity> getPaginatedEntityTypeList(CommandGetListEntityType command) throws Exception;

    boolean delete(CommandGetListEntityType command) throws Exception;
}
