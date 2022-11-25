package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.pattern.command.CommandPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAdd;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternDelete;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternUpdate;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;

import java.util.List;

public interface IPatternService extends IBaseService {
    ResponsePatternAdd add(CommandPatternAdd command);
    ResponsePattern delete(CommandPatternDelete command);
    ResponsePattern getByIntentId(String intentId,String userId);

    List<PatternEntity> addMany(List<PatternEntity> patternsToAdd);

    ResponsePattern getById(String id, String userId);

    ResponsePattern getByUserId(String userId);

    ResponsePattern update(CommandPatternUpdate command);
}
