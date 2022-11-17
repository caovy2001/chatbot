package com.caovy2001.chatbot.service.training_history;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.TrainingHistoryEntity;
import com.caovy2001.chatbot.repository.TrainingHistoryRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistory;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistoryAdd;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistory;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistoryAdd;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TrainingHistoryService extends BaseService implements ITrainingHistoryService {

    @Autowired
    private TrainingHistoryRepository trainingHistoryRepository;

    @Override
    public ResponseTrainingHistoryAdd add(CommandTrainingHistoryAdd command) {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getUsername())) {
            return this.returnException(ExceptionConstant.missing_param, ResponseTrainingHistoryAdd.class);
        }

        TrainingHistoryEntity trainingHistoryEntity = trainingHistoryRepository.insert(TrainingHistoryEntity.builder()
                        .username(command.getUsername())
                        .userId(command.getUserId())
                .build());

        return ResponseTrainingHistoryAdd.builder()
                .id(trainingHistoryEntity.getId())
                .userId(trainingHistoryEntity.getUserId())
                .username(trainingHistoryEntity.getUsername())
                .build();
    }

    @Override
    public ResponseTrainingHistory updateStatus(CommandTrainingHistory command) {
        if (StringUtils.isAnyBlank(command.getId(), command.getUserId()) ||
        command.getStatus() == null) {
            return this.returnException(ExceptionConstant.missing_param, ResponseTrainingHistory.class);
        }

        TrainingHistoryEntity trainingHistoryEntity = trainingHistoryRepository.findByIdAndUserId(command.getId(), command.getUserId());
        if (trainingHistoryEntity == null) {
            return this.returnException("training_history_null", ResponseTrainingHistory.class);
        }

        trainingHistoryEntity.setStatus(command.getStatus());
        TrainingHistoryEntity updatedTrainingHistory = trainingHistoryRepository.save(trainingHistoryEntity);
        return ResponseTrainingHistory.builder()
                .id(updatedTrainingHistory.getId())
                .status(updatedTrainingHistory.getStatus())
                .build();
    }
}
