package com.caovy2001.chatbot.service.message_history_group;

import com.caovy2001.chatbot.entity.MessageHistoryGroupEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.message_history_group.command.CommandGetListMessageHistoryGroup;

public interface IMessageHistoryGroupServiceAPI extends IBaseService {
    Paginated<MessageHistoryGroupEntity> getPaginatedList(CommandGetListMessageHistoryGroup command) throws Exception;
}
