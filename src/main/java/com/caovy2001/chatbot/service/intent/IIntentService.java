package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.intent.command.CommandIntent;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;

public interface IIntentService extends IBaseService {
    ResponseIntentAdd add(CommandIntent command);

    ResponseIntents getByUserId(String userId);

    ResponseIntents getById(String id,String userId);

    ResponseIntents deleteIntent(String id,String userId);

    ResponseIntents updateName(CommandIntent command,String userId);
}
