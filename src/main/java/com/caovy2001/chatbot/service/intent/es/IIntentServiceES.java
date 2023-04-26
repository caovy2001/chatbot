package com.caovy2001.chatbot.service.intent.es;

import com.caovy2001.chatbot.service.intent.command.CommandDeleteIntentES;
import com.caovy2001.chatbot.service.intent.command.CommandIndexingIntentES;

public interface IIntentServiceES {
    void index(CommandIndexingIntentES command) throws Exception;

    void delete(CommandDeleteIntentES command) throws Exception;
}
