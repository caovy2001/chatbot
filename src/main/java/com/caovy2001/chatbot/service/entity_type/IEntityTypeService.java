package com.caovy2001.chatbot.service.entity_type;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.entity_type.command.CommandAddEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandUpdateEntityType;

import java.util.List;

public interface IEntityTypeService extends IBaseService {
    //    List<EntityTypeEntity> add(CommandEntityTypeAddMany command);
    <Entity extends BaseEntity, CommandAddMany extends CommandAddManyBase> List<Entity> add(CommandAddMany commandAddManyBase) throws Exception;

    <Entity extends BaseEntity, CommandAdd extends CommandAddBase> Entity add(CommandAdd commandAddBase) throws Exception;

    <Entity extends BaseEntity, CommandUpdate extends CommandUpdateBase> Entity update(CommandUpdate commandUpdateBase) throws Exception;

    <CommandGetList extends CommandGetListBase> boolean delete(CommandGetList commandGetListBase) throws Exception;

//    List<EntityTypeEntity> getList(CommandGetListEntityType command);

//    EntityTypeEntity add(CommandAddEntityType command) throws Exception;

//    EntityTypeEntity update(CommandUpdateEntityType command) throws Exception;

//    Paginated<EntityTypeEntity> getPaginatedEntityTypeList(CommandGetListEntityType command) throws Exception;

//    boolean delete(CommandGetListEntityType command) throws Exception;
}
