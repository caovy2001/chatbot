package com.caovy2001.chatbot.service.message_history_group;

import com.caovy2001.chatbot.entity.MessageHistoryGroupEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.message_history_group.command.CommandAddMessageHistoryGroup;
import com.caovy2001.chatbot.service.message_history_group.command.CommandGetListMessageHistoryGroup;

import java.util.List;

public interface IMessageHistoryGroupService extends IBaseService {
    MessageHistoryGroupEntity add(CommandAddMessageHistoryGroup command);
    List<MessageHistoryGroupEntity> getList(CommandGetListMessageHistoryGroup command);
}
