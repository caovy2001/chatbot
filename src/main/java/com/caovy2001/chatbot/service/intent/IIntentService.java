package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.intent.command.*;

public interface IIntentService extends IBaseService {
    Boolean suggestPattern(CommandIntentSuggestPattern command) throws Exception;
    String askGpt(String message);
    void groupEntityType(String userId, String intentId) throws Exception;
}
