package com.caovy2001.chatbot.service.training;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.*;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import com.caovy2001.chatbot.service.jedis.IJedisService;
import com.caovy2001.chatbot.service.jedis.JedisService;
import com.caovy2001.chatbot.service.node.INodeService;
import com.caovy2001.chatbot.service.script.IScriptService;
import com.caovy2001.chatbot.service.training.command.CommandTrainingPredict;
import com.caovy2001.chatbot.service.training.command.CommandTrainingTrain;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingPredict;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingPredictFromAI;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingServerStatus;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingTrain;
import com.caovy2001.chatbot.service.training_history.ITrainingHistoryService;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistory;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistoryAdd;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistory;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistoryAdd;
import com.caovy2001.chatbot.service.user.IUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrainingService extends BaseService implements ITrainingService {
    @Autowired
    private IIntentService intentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ITrainingHistoryService trainingHistoryService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IScriptService scriptService;

    @Autowired
    private INodeService nodeService;

    @Autowired
    private IJedisService jedisService;

    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("custom");

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
        try {
            command.setIntents(responseIntents.getIntents());
            command.setTrainingHistoryId(responseTrainingHistoryAdd.getId());
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String commandBody = objectMapper.writeValueAsString(command);
            log.info("[train]: Send training request: {}", commandBody);

            CompletableFuture.runAsync(() -> {
                try {
                    jedisService.set(command.getUserId() + JedisService.PrefixRedisKey.COLON + JedisService.PrefixRedisKey.trainingServerStatus, "busy");
                    HttpEntity<String> request =
                            new HttpEntity<>(commandBody, headers);
                    restTemplate.postForLocation(new URI(resourceBundle.getString("training.server") + "/train"), request);

                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            });
        } catch (Throwable throwable) {
            log.error("[{}|train]: {}", command.getUserId(), throwable.getMessage());
        }
        return ResponseTrainingTrain.builder()
                .trainingHistoryId(responseTrainingHistoryAdd.getId())
                .build();
    }

    @Override
    public ResponseTrainingPredict predict(CommandTrainingPredict command) {
        // Lay user tu secret key
        UserEntity userEntity = userService.getBySecretKey(command.getSecretKey());
        if (userEntity == null) {
            return this.returnException("user_not_exist", ResponseTrainingPredict.class);
        }

        ScriptEntity script = scriptService.getScriptById(command.getScriptId());
        if (script == null || !script.getUserId().equals(userEntity.getId())) {
            return this.returnException(ExceptionConstant.error_occur, ResponseTrainingPredict.class);
        }

        List<NodeEntity> nodes = nodeService.getAllByScriptId(script.getId());
        if (CollectionUtils.isEmpty(nodes)) {
            return this.returnException(ExceptionConstant.error_occur, ResponseTrainingPredict.class);
        }

        NodeEntity currNode = null;
        if ("_BEGIN".equals(command.getCurrentNodeId())) {
            currNode = nodes.stream().filter(NodeEntity::getIsFirstNode).findFirst().orElse(null);
            if (currNode == null) return this.returnException("script_not_have_first_node", ResponseTrainingPredict.class);

            return ResponseTrainingPredict.builder()
                    .currentNodeId(currNode.getNodeId())
                    .message(currNode.getMessage())
                    .build();
        } else {
            currNode = nodes.stream()
                    .filter(nodeEntity -> nodeEntity.getNodeId().equals(command.getCurrentNodeId())).findFirst().orElse(null);
        }

        if (currNode == null) {
            return this.returnException("node_id_not_exist", ResponseTrainingPredict.class);
        }

        if (CollectionUtils.isEmpty(currNode.getConditionMappings())) {
            return ResponseTrainingPredict.builder()
                    .currentNodeId("_END")
                    .message(script.getEndMessage())
                    .build();
        }

        // Kiem tra keyword
        List<ConditionMappingEntity> keywordCMs = currNode.getConditionMappings().stream()
                .filter(cm -> ConditionMappingEntity.EPredictType.KEYWORD.equals(cm.getPredictType())).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(keywordCMs)) {
            for (ConditionMappingEntity cm: keywordCMs) {
                if (CollectionUtils.isEmpty(cm.getNext_node_ids())) continue;

                if (command.getMessage().toLowerCase().contains(cm.getKeyword().toLowerCase())) {
                    NodeEntity nextNode = nodes.stream()
                            .filter(nodeEntity -> nodeEntity.getNodeId().equals(cm.getNext_node_ids().get(0))).findFirst().orElse(null);
                    if (nextNode == null) continue;

                    return ResponseTrainingPredict.builder()
                            .currentNodeId(nextNode.getNodeId())
                            .message(nextNode.getMessage())
                            .build();
                }
            }
        }

        // Predict
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<String> intentIds = currNode.getConditionMappings().stream()
                .filter(cm -> ConditionMappingEntity.EPredictType.INTENT.equals(cm.getPredictType()))
                .map(ConditionMappingEntity::getIntentId).collect(Collectors.toList());
        Map<String, Object> commandRequest = new HashMap<>();
        commandRequest.put("text", command.getMessage());
        commandRequest.put("username", userEntity.getUsername());
        commandRequest.put("user_id", userEntity.getId());
        commandRequest.put("intent_ids", intentIds);

        String commandBody = null;
        try {
            commandBody = objectMapper.writeValueAsString(commandRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        HttpEntity<String> request =
                new HttpEntity<>(commandBody, headers);
        ResponseTrainingPredictFromAI responseTrainingPredictFromAI =
                restTemplate.postForObject(resourceBundle.getString("training.server") + "/predict", request, ResponseTrainingPredictFromAI.class);

        if (responseTrainingPredictFromAI == null) {
            return this.returnException(ExceptionConstant.error_occur, ResponseTrainingPredict.class);
        }

        String intentId = responseTrainingPredictFromAI.getIntentId();
        if (StringUtils.isBlank(intentId)) {
            return ResponseTrainingPredict.builder()
                    .currentNodeId(currNode.getNodeId())
                    .message(script.getWrongMessage())
                    .build();
        }

        ConditionMappingEntity conditionMappingEntity = currNode.getConditionMappings().stream()
                .filter(cm -> intentId.equals(cm.getIntentId())).findFirst().orElse(null);
        if (conditionMappingEntity == null) {
            return ResponseTrainingPredict.builder()
                    .currentNodeId(currNode.getNodeId())
                    .message(script.getWrongMessage())
                    .build();
        }

        if (CollectionUtils.isEmpty(conditionMappingEntity.getNext_node_ids())) {
            return ResponseTrainingPredict.builder()
                    .currentNodeId("_END")
                    .message(script.getEndMessage())
                    .build();
        }

        String nextNodeId = conditionMappingEntity.getNext_node_ids().get(0);
        NodeEntity nextNode = nodes.stream()
                .filter(nodeEntity -> nodeEntity.getNodeId().equals(nextNodeId)).findFirst().orElse(null);
        if (nextNode == null) {
            return ResponseTrainingPredict.builder()
                    .currentNodeId("_END")
                    .message(script.getEndMessage())
                    .build();
        }

        return ResponseTrainingPredict.builder()
                .currentNodeId(nextNodeId)
                .message(nextNode.getMessage())
                .build();
    }

    @Override
    public Boolean trainDone(CommandTrainingTrain command) {
        ResponseTrainingHistory responseTrainingHistory = trainingHistoryService.updateStatus(CommandTrainingHistory.builder()
                .id(command.getTrainingHistoryId())
                .userId(command.getUserId())
                .status(TrainingHistoryEntity.EStatus.SUCCESS)
                .build());

        if (responseTrainingHistory == null ||
                !TrainingHistoryEntity.EStatus.SUCCESS.equals(responseTrainingHistory.getStatus())) {
            return false;
        }

        jedisService.set("training_server_status", "free");
        return true;
    }

    @Override
    public ResponseTrainingServerStatus getServerStatus(String userId) {
        if (StringUtils.isBlank(userId)) {
            return returnException(ExceptionConstant.missing_param, ResponseTrainingServerStatus.class);
        }

        String status = jedisService.get(userId + JedisService.PrefixRedisKey.COLON + JedisService.PrefixRedisKey.trainingServerStatus);
        if (StringUtils.isBlank(status) || status.equals(ResponseTrainingServerStatus.EStatus.FREE.name().toLowerCase())) {
            return ResponseTrainingServerStatus.builder()
                    .status(ResponseTrainingServerStatus.EStatus.FREE)
                    .build();
        }

        return ResponseTrainingServerStatus.builder()
                .status(ResponseTrainingServerStatus.EStatus.BUSY)
                .build();
    }
}
