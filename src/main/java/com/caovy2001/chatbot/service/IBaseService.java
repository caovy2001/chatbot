package com.caovy2001.chatbot.service;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;

import java.util.List;

public interface IBaseService {
    <T extends ResponseBase> T returnException(String exceptionCode, Class<T> clazz);

    <E extends BaseEntity, Command extends CommandGetListBase> Paginated<E> getPaginatedList(Command command, Class<E> entityClass, Class<Command> commandClass) throws Exception;

    <T> List<T> getList(CommandGetListBase command, Class<T> entityType);
}
