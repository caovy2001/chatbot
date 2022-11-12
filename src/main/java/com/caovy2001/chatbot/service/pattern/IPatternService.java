package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.pattern.command.CommandPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAdd;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;

public interface IPatternService extends IBaseService {
    ResponsePatternAdd add(CommandPatternAdd command);
    ResponsePatternAdd delete(String command);
    ResponsePattern getByIntentId(CommandPattern command);
}
