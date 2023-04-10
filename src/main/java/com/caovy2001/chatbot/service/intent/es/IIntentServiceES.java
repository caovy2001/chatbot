package com.caovy2001.chatbot.service.intent.es;

import com.caovy2001.chatbot.service.intent.command.CommandIndexingIntentES;

public interface IIntentServiceES {
    void processIndexing(CommandIndexingIntentES command) throws Exception;
}
