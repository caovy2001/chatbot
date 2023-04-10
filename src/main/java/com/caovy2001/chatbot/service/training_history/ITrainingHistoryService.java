package com.caovy2001.chatbot.service.training_history;

import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistory;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistoryAdd;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistory;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistoryAdd;

public interface ITrainingHistoryService extends IBaseService {
    ResponseTrainingHistoryAdd add(CommandTrainingHistoryAdd command);

    ResponseTrainingHistory updateStatus(CommandTrainingHistory command);
}
