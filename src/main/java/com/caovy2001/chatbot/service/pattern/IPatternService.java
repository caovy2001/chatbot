package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.pattern.command.*;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;

import java.util.List;

public interface IPatternService extends IBaseService {
    <Entity extends BaseEntity, CommandAdd extends CommandAddBase> Entity add(CommandAdd commandAddBase) throws Exception;

    //    ResponsePatternAdd add(CommandPatternAdd command) throws Exception;
    <Entity extends BaseEntity, CommandAddMany extends CommandAddManyBase> List<Entity> add(CommandAddMany commandAddManyBase) throws Exception;

    <Entity extends BaseEntity, CommandUpdate extends CommandUpdateBase> Entity update(CommandUpdate commandUpdateBase) throws Exception;

//    List<PatternEntity> add(CommandPatternAddMany command) throws Exception;

    <CommandGetList extends CommandGetListBase> boolean delete(CommandGetList commandGetListBase) throws Exception;

//    ResponsePattern delete(CommandPatternDelete command);

//    boolean delete(CommandGetListPattern command);

//    ResponsePattern getByIntentId(String intentId,String userId);

//    List<PatternEntity> addMany(List<PatternEntity> patternsToAdd);

//    List<PatternEntity> addMany(CommandPatternAddMany commandPatternAddMany) throws Exception;

//    ResponsePattern getById(String id, String userId);

//    ResponsePattern getByUserId(String userId);

//    ResponsePattern update(CommandPatternUpdate command) throws Exception;

//    @Deprecated
//    Paginated<PatternEntity> getPagination(String userId, int page, int size);

//    @Deprecated
//    Paginated<PatternEntity> getPaginationByIntentId(String intentId, int page, int size);

    void importFromFile(CommandImportPatternsFromFile command) throws Exception;

//    List<PatternEntity> getList(CommandGetListPattern command);

    void exportExcel(CommandGetListPattern command, String sessionId) throws Exception;
}
