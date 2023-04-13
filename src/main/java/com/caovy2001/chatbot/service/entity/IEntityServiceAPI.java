package com.caovy2001.chatbot.service.entity;


import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;

public interface IEntityServiceAPI extends IBaseService {

    Paginated<EntityEntity> getPaginatedList(CommandGetListEntity command) throws Exception;
}
