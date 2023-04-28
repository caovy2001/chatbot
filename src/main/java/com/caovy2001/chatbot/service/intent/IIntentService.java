package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.intent.command.*;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;

import java.util.List;

public interface IIntentService extends IBaseService {
//    ResponseIntentAdd add(CommandIntent command);
    IntentEntity add(CommandIntentAdd command) throws Exception;
    List<IntentEntity> add(CommandIntentAddMany command) throws Exception;

//    ResponseIntentAdd addMany(CommandIntentAddMany command);

    ResponseIntents getByUserId(String userId);

    ResponseIntents getById(String id, String userId);

    ResponseIntents deleteIntent(String id, String userId);

    ResponseIntents updateName(CommandIntent command, String userId);

    ResponseIntents update(CommandIntent command);

    ResponseIntents addPatterns(CommandIntentAddPattern command);

    Paginated<IntentEntity> getPaginationByUserId(String userId, int page, int size);
    Paginated<IntentEntity> getPagination(CommandGetListIntent command);

//    List<IntentEntity> addManyReturnList(CommandIntentAddMany commandIntentAddMany) throws Exception;
    List<IntentEntity> getList(CommandGetListIntent command);
}
