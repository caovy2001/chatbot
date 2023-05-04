package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.pattern.command.CommandGetListPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandImportPatternsFromFile;

import java.util.List;

public interface IPatternService extends IBaseService {
    <Entity extends BaseEntity, CommandAdd extends CommandAddBase> Entity add(CommandAdd commandAddBase) throws Exception;

    <Entity extends BaseEntity, CommandAddMany extends CommandAddManyBase> List<Entity> add(CommandAddMany commandAddManyBase) throws Exception;

    <Entity extends BaseEntity, CommandUpdate extends CommandUpdateBase> Entity update(CommandUpdate commandUpdateBase) throws Exception;

    <CommandGetList extends CommandGetListBase> boolean delete(CommandGetList commandGetListBase) throws Exception;

    void importFromFile(CommandImportPatternsFromFile command) throws Exception;

    void exportExcel(CommandGetListPattern command, String sessionId) throws Exception;
}
