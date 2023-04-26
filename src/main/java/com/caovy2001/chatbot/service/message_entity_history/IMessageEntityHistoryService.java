package com.caovy2001.chatbot.service.message_entity_history;

import com.caovy2001.chatbot.entity.MessageEntityHistoryEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.message_entity_history.command.CommandAddMessageEntityHistory;
import com.caovy2001.chatbot.service.message_entity_history.command.CommandAppendValuesMessageEntityHistory;
import com.caovy2001.chatbot.service.message_entity_history.command.CommandGetListMessageEntityHistory;

import java.util.List;

public interface IMessageEntityHistoryService extends IBaseService {
    MessageEntityHistoryEntity add(CommandAddMessageEntityHistory command);
    List<MessageEntityHistoryEntity> getList(CommandGetListMessageEntityHistory command);
    MessageEntityHistoryEntity checkExistAndAdd(CommandGetListMessageEntityHistory commandCheckExist, CommandAddMessageEntityHistory commandAdd);
    MessageEntityHistoryEntity appendValues(CommandAppendValuesMessageEntityHistory command);
}
