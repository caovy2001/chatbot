package com.caovy2001.chatbot.service.node;

import com.caovy2001.chatbot.entity.NodeEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.node.command.CommandNodeAdd;
import com.caovy2001.chatbot.service.node.command.CommandNodeAddConditionMapping;
import com.caovy2001.chatbot.service.node.command.CommandUpdateMessage;
import com.caovy2001.chatbot.service.node.response.ResponseListNode;
import com.caovy2001.chatbot.service.node.response.ResponseNode;

import java.util.List;

public interface INodeService extends IBaseService {
    ResponseNode add(CommandNodeAdd command);

    ResponseListNode get(String id, String userId);

    ResponseNode addConditionMapping(CommandNodeAddConditionMapping command);

    ResponseNode addNextNode(CommandNodeAddConditionMapping command);

    ResponseNode removeNextNode(CommandNodeAddConditionMapping command);

    ResponseNode updateNextNode(CommandNodeAddConditionMapping command);

    ResponseNode updateMessage(CommandUpdateMessage command);

    ResponseNode delete(String id);

    void deleteMany(List<String> ids);

    List<NodeEntity> getAllByScriptId(String scriptId);

    List<NodeEntity> addMany(List<NodeEntity> nodes);
}
