package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.intent.command.CommandIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;

public interface IIntentService extends IBaseService {
    ResponseIntentAdd add(CommandIntentAdd command);

    ResponseIntents getByUserId(String userId);

    ResponseIntents getById(String id,String userId);
}
