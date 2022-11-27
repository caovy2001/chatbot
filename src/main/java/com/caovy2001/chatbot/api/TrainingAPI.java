package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.script.response.ResponseScriptAdd;
import com.caovy2001.chatbot.service.training.ITrainingService;
import com.caovy2001.chatbot.service.training.command.CommandTrainingPredict;
import com.caovy2001.chatbot.service.training.command.CommandTrainingTrain;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingPredict;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingServerStatus;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingTrain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/training")
@Slf4j
public class TrainingAPI {
    @Autowired
    private ITrainingService trainingService;

    @Autowired
    private IBaseService baseService;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/train")
    public ResponseEntity<ResponseTrainingTrain> train() {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            CommandTrainingTrain command = CommandTrainingTrain.builder().build();
            command.setUserId(userEntity.getId());
            command.setUsername(userEntity.getUsername());

            ResponseTrainingTrain response = trainingService.train(command);
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(), ResponseTrainingTrain.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_server_status")
    public ResponseEntity<ResponseTrainingServerStatus> getServerStatus() {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            ResponseTrainingServerStatus response = trainingService.getServerStatus(userEntity.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(), ResponseTrainingServerStatus.class));
        }
    }

    @PostMapping("/predict")
    public ResponseEntity<ResponseTrainingPredict> predict(@RequestBody CommandTrainingPredict command) {
        try {
            if (StringUtils.isBlank(command.getSecretKey())) {
                throw new Exception("auth_invalid");
            }

            if (StringUtils.isAnyBlank(command.getScriptId(), command.getCurrentNodeId()) ||
                    (StringUtils.isBlank(command.getMessage()) && !"_BEGIN".equals(command.getCurrentNodeId()))) {
                throw new Exception(ExceptionConstant.missing_param);
            }

            ResponseTrainingPredict response = trainingService.predict(command);
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(), ResponseTrainingPredict.class));
        }
    }

    @PostMapping("/train_done")
    public ResponseEntity<Boolean> trainDone(@RequestBody CommandTrainingTrain command) {
        log.info("[trainDone] Receive message: {}", command);
        if (StringUtils.isBlank(command.getTrainingHistoryId())) {
            log.info("[trainDone]: training_history_id_null");
            return ResponseEntity.ok(false);
        }

        return ResponseEntity.ok(trainingService.trainDone(command));
    }
}
