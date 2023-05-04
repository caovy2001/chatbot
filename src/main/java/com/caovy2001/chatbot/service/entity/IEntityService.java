package com.caovy2001.chatbot.service.entity;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;

import java.util.List;

public interface IEntityService extends IBaseService {
//    List<EntityEntity> add(List<CommandAddEntity> commandAddEntities);

//    List<EntityEntity> findByUserIdAndPatternId(String userId, String patternId);

    <Entity extends BaseEntity, CommandAddMany extends CommandAddManyBase> List<Entity> add(CommandAddMany commandAddManyBase) throws Exception;

    //    List<EntityEntity> addMany(CommandEntityAddMany command);
    <CommandGetList extends CommandGetListBase> boolean delete(CommandGetList commandGetListBase) throws Exception;

//    boolean delete(CommandGetListEntity command);
}
