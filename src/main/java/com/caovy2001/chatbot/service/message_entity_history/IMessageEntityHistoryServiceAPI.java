package com.caovy2001.chatbot.service.message_entity_history;

import com.caovy2001.chatbot.entity.MessageEntityHistoryEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.message_entity_history.command.CommandGetListMessageEntityHistory;

public interface IMessageEntityHistoryServiceAPI extends IBaseService {
    Paginated<MessageEntityHistoryEntity> getPaginatedList(CommandGetListMessageEntityHistory command) throws Exception;
}
