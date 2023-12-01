package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.intent.command.*;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;

import java.util.List;

public interface IIntentService extends IBaseService {
    Boolean suggestPattern(CommandIntentSuggestPattern command) throws Exception;
}
