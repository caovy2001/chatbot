package com.caovy2001.chatbot.service.message_history;

import com.caovy2001.chatbot.entity.MessageHistoryEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.message_history.command.CommandAddMessageHistory;
import com.caovy2001.chatbot.service.message_history.command.CommandGetListMessageHistory;

import java.util.List;

public interface IMessageHistoryService extends IBaseService {
    MessageHistoryEntity add(CommandAddMessageHistory command);
    List<MessageHistoryEntity> getList(CommandGetListMessageHistory command);
    Paginated<MessageHistoryEntity> getPaginatedListBySessionIdAndScriptId(CommandGetListMessageHistory command) throws Exception; // For API
    Paginated<MessageHistoryEntity> getPaginatedList(CommandGetListMessageHistory command) throws Exception;
}
