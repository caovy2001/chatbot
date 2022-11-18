package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.script.response.ResponseScriptAdd;
import com.caovy2001.chatbot.service.training.ITrainingService;
import com.caovy2001.chatbot.service.training.command.CommandTrainingPredict;
import com.caovy2001.chatbot.service.training.command.CommandTrainingTrain;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingPredict;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingTrain;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/training")
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

    @PostMapping("/train")
    public ResponseEntity<ResponseTrainingPredict> predict(@RequestBody CommandTrainingPredict command) {
        try {
            if (StringUtils.isBlank(command.getSecretKey())) {
                throw new Exception("auth_invalid");
            }

            if (StringUtils.isAnyBlank(command.getMessage(), command.getScriptId(), command.getCurrentNodeId())) {
                throw new Exception(ExceptionConstant.missing_param);
            }

            ResponseTrainingPredict response = trainingService.predict(command);
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(), ResponseTrainingPredict.class));
        }
    }
}
