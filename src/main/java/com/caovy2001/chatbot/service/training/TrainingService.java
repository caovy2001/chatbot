package com.caovy2001.chatbot.service.training;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.TrainingHistoryEntity;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import com.caovy2001.chatbot.service.training.command.CommandTrainingTrain;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingTrain;
import com.caovy2001.chatbot.service.training_history.ITrainingHistoryService;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistory;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistoryAdd;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistory;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistoryAdd;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class TrainingService extends BaseService implements ITrainingService {
    @Autowired
    private IIntentService intentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ITrainingHistoryService trainingHistoryService;

    @Override
    public ResponseTrainingTrain train(CommandTrainingTrain command) {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getUsername())) {
            return returnException(ExceptionConstant.missing_param, ResponseTrainingTrain.class);
        }

        ResponseIntents responseIntents = intentService.getByUserId(command.getUserId());
        if (responseIntents == null || CollectionUtils.isEmpty(responseIntents.getIntents())) {
            return returnException("intents_empty", ResponseTrainingTrain.class);
        }

        // Lưu training history
        ResponseTrainingHistoryAdd responseTrainingHistoryAdd = trainingHistoryService.add(CommandTrainingHistoryAdd.builder()
                .userId(command.getUserId())
                .username(command.getUsername())
                .build());

        if (responseTrainingHistoryAdd == null || StringUtils.isBlank(responseTrainingHistoryAdd.getId())) {
            return returnException("add_training_history_fail", ResponseTrainingTrain.class);
        }

        // Gửi request sang python để thực hiên train
        CompletableFuture.runAsync(() -> {
            try {
                command.setIntents(responseIntents.getIntents());
                command.setTrainingHistoryId(responseTrainingHistoryAdd.getId());
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                String commandBody = objectMapper.writeValueAsString(command);
                HttpEntity<String> request =
                        new HttpEntity<>(commandBody, headers);
                ResponseTrainingTrain responseTrainingTrain =
                        restTemplate.postForObject("http://localhost:5000/train", request, ResponseTrainingTrain.class);

                if (responseTrainingTrain == null || StringUtils.isBlank(responseTrainingTrain.getTrainingHistoryId())) {
                    throw new Exception("train_fail");
                }
                ResponseTrainingHistory responseTrainingHistory = trainingHistoryService.updateStatus(CommandTrainingHistory.builder()
                        .id(responseTrainingTrain.getTrainingHistoryId())
                        .userId(command.getUserId())
                        .status(TrainingHistoryEntity.EStatus.SUCCESS)
                        .build());

                if (responseTrainingHistory == null) {
                    throw new Exception("update_status_training_history_fail");
                }
            } catch (Throwable throwable) {
                log.error("[{}|train]: {}", command.getUserId(), throwable.getMessage());
            }
        }, Executors.newSingleThreadExecutor());

        return ResponseTrainingTrain.builder()
                .trainingHistoryId(responseTrainingHistoryAdd.getId())
                .build();
    }
}
