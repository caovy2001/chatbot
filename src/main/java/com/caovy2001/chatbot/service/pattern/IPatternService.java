package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.pattern.command.CommandPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAdd;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;

import java.util.List;

public interface IPatternService extends IBaseService {
    ResponsePatternAdd add(CommandPatternAdd command);
    ResponsePatternAdd delete(String command);
    ResponsePattern getByIntentId(String intentId,String userId);

    List<PatternEntity> addMany(List<PatternEntity> patternsToAdd);
}
