package com.caovy2001.chatbot.service.script;

import com.caovy2001.chatbot.entity.ScriptEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.script.command.CommandScriptAdd;
import com.caovy2001.chatbot.service.script.command.CommandScriptDelete;
import com.caovy2001.chatbot.service.script.command.CommandScriptUpdate;
import com.caovy2001.chatbot.service.script.response.ResponseScript;
import com.caovy2001.chatbot.service.script.response.ResponseScriptAdd;
import com.caovy2001.chatbot.service.script.response.ResponseScriptGetByUserId;

import java.util.List;

public interface IScriptService extends IBaseService {
    ResponseScriptAdd add(CommandScriptAdd command);
    ResponseScriptGetByUserId getScriptByUserId(String userId);
    ScriptEntity getScriptById(String id);
    ResponseScript updateName(CommandScriptUpdate command);
    ResponseScript deleteScript(String id);

    ResponseScript update(CommandScriptUpdate command);
}
