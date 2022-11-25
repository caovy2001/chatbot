package com.caovy2001.chatbot.service.training;

import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.training.command.CommandTrainingPredict;
import com.caovy2001.chatbot.service.training.command.CommandTrainingTrain;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingPredict;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingServerStatus;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingTrain;

public interface ITrainingService extends IBaseService {
    ResponseTrainingTrain train(CommandTrainingTrain command);

    ResponseTrainingPredict predict(CommandTrainingPredict command);

    Boolean trainDone(CommandTrainingTrain command);

    ResponseTrainingServerStatus getServerStatus(String userId);
}
