package com.caovy2001.chatbot.service.pattern.es;

import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.pattern.command.CommandIndexingPatternES;

public interface IPatternServiceES extends IBaseService {
    void processIndexing(CommandIndexingPatternES command) throws Exception;
}
