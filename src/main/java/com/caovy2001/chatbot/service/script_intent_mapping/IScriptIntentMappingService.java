package com.caovy2001.chatbot.service.script_intent_mapping;

import com.caovy2001.chatbot.entity.ScriptIntentMappingEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.script_intent_mapping.command.CommandGetListScriptIntentMapping;

import java.util.List;

public interface IScriptIntentMappingService extends IBaseService {
    List<ScriptIntentMappingEntity> getList(CommandGetListScriptIntentMapping command);

    boolean addForScriptIdByIntentIds(String userId, String scriptId, List<String> intentIds);

    boolean deleteByScriptId(String userId, String scriptId);
}
