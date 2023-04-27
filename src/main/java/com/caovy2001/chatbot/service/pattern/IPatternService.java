package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.pattern.command.*;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;

import java.util.List;

public interface IPatternService extends IBaseService {
    ResponsePatternAdd add(CommandPatternAdd command) throws Exception;
    ResponsePattern delete(CommandPatternDelete command);
    boolean delete(CommandGetListPattern command);
    ResponsePattern getByIntentId(String intentId,String userId);

    List<PatternEntity> addMany(List<PatternEntity> patternsToAdd);

    List<PatternEntity> addMany(CommandPatternAddMany commandPatternAddMany);

    ResponsePattern getById(String id, String userId);

    ResponsePattern getByUserId(String userId);

    ResponsePattern update(CommandPatternUpdate command) throws Exception;

    @Deprecated
    Paginated<PatternEntity> getPagination(String userId, int page, int size);

    @Deprecated
    Paginated<PatternEntity> getPaginationByIntentId(String intentId, int page, int size);

    void importFromFile(CommandImportPatternsFromFile command) throws Exception;

//    List<PatternEntity> getList(CommandGetListPattern command);

    void exportExcel(CommandGetListPattern command, String sessionId) throws Exception;
}
