package com.caovy2001.chatbot.service.entity;

import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;

import java.util.List;

public interface IEntityService extends IBaseService {
    List<EntityEntity> add(List<CommandAddEntity> commandAddEntities);

    List<EntityEntity> findByUserIdAndPatternId(String userId, String patternId);

    List<EntityEntity> addMany(CommandEntityAddMany command);
    List<EntityEntity> getList(CommandGetListEntity command);
}
