package com.caovy2001.chatbot.service.training;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.*;
import com.caovy2001.chatbot.enumeration.EMessageHistoryFrom;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.command.CommandGetListIntent;
import com.caovy2001.chatbot.service.jedis.IJedisService;
import com.caovy2001.chatbot.service.jedis.JedisService;
import com.caovy2001.chatbot.service.message_history.IMessageHistoryService;
import com.caovy2001.chatbot.service.message_history.command.CommandAddMessageHistory;
import com.caovy2001.chatbot.service.node.INodeService;
import com.caovy2001.chatbot.service.script.IScriptService;
import com.caovy2001.chatbot.service.script.command.CommandGetListScript;
import com.caovy2001.chatbot.service.training.command.*;
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
import com.caovy2001.chatbot.service.user.command.CommandGetListUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.caovy2001.chatbot.service.jedis.JedisService.PrefixRedisKey.COLON;

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

    @Autowired
    private IEntityTypeService entityTypeService;

    @Autowired
    private IMessageHistoryService messageHistoryService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("custom");

    @Override
    public ResponseTrainingTrain train(CommandTrainingTrain command) {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getUsername())) {
            return returnException(ExceptionConstant.missing_param, ResponseTrainingTrain.class);
        }

        List<IntentEntity> intentEntities = intentService.getList(CommandGetListIntent.builder()
                .userId(command.getUserId())
                .hasPatterns(true)
                .hasEntitiesOfPatterns(true)
                .hasEntityTypesOfEntitiesOfPatterns(true)
                .build(), IntentEntity.class);
        if (CollectionUtils.isEmpty(intentEntities)) {
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
            command.setIntents(intentEntities);
            command.setTrainingHistoryId(responseTrainingHistoryAdd.getId());
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String commandBody = objectMapper.writeValueAsString(command);
//            log.info("[train]: Send training request: {}", commandBody);

            CompletableFuture.runAsync(() -> {
                try {
                    jedisService.set(command.getUserId() + COLON + JedisService.PrefixRedisKey.trainingServerStatus, "busy");
                    HttpEntity<String> request =
                            new HttpEntity<>(commandBody, headers);
                    restTemplate.postForLocation(new URI(resourceBundle.getString("training.server") + "/train"), request);

                } catch (Exception e) {
                    log.error(e.getMessage());
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
    public ResponseTrainingPredict predict(CommandTrainingPredict command) throws Exception {
        //region Lay user tu secret key
        UserEntity userEntity = this.getUserForPredictSession(command.getSecretKey());
        if (userEntity == null) {
            return this.returnException("user_not_exist", ResponseTrainingPredict.class);
        }
        //endregion

        //region Lấy script
        ScriptEntity script = this.getScriptForPredictSession(userEntity.getId(), command.getScriptId());
        if (script == null) {
            return this.returnException("script_null", ResponseTrainingPredict.class);
        }
        if (CollectionUtils.isEmpty(script.getNodes())) {
            return this.returnException("nodes_empty", ResponseTrainingPredict.class);
        }
        final String wrongMessage = script.getWrongMessage();
        final String endMessage = script.getEndMessage();
        //endregion

        //region Lưu message đầu tiên mà người dùng gửi lên
        if (BooleanUtils.isFalse(command.getIsTrying())) {
            kafkaTemplate.send(Constant.KafkaTopic.process_save_message_when_predict, objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(userEntity.getId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                    .nodeId(command.getCurrentNodeId())
                    .message(command.getMessage())
                    .from(EMessageHistoryFrom.CUSTOMER)
                    .checkAddMessageHistoryGroup(true)
                    .saveMessageEntityHistory(true)
                    .build()));
        }
        //endregion

        //region Đổ node vào map => Phục vụ việc search
        NodeEntity firstNode = null;
        Map<String, NodeEntity> nodesByNodeId = new HashMap<>();
        for (NodeEntity node : script.getNodes()) {
            nodesByNodeId.put(node.getNodeId(), node);
            if (BooleanUtils.isTrue(node.getIsFirstNode())) {
                firstNode = node;
            }
        }
        if (firstNode == null)
            return this.returnException("script_not_have_first_node", ResponseTrainingPredict.class);
        //endregion

        //region Nếu đây là câu bắt đầu thì trả về message của node đầu tiên
        if ("_BEGIN".equals(command.getCurrentNodeId())) {
            // Lưu message mà bot gửi đi
            if (BooleanUtils.isFalse(command.getIsTrying())) {
                kafkaTemplate.send(Constant.KafkaTopic.process_save_message_when_predict, objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(userEntity.getId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                        .nodeId(firstNode.getNodeId())
                        .message(firstNode.getMessage())
                        .from(EMessageHistoryFrom.BOT)
                        .checkAddMessageHistoryGroup(true)
                        .saveMessageEntityHistory(true)
                        .build()));
            }

            return ResponseTrainingPredict.builder()
                    .currentNodeId(firstNode.getNodeId())
                    .message(firstNode.getMessage())
                    .build();
        }
        //endregion

        //region Lấy ra node hiện tại
        NodeEntity currNode = nodesByNodeId.get(command.getCurrentNodeId());
        if (currNode == null) {
            return this.returnException("node_id_not_exist", ResponseTrainingPredict.class);
        }
        //endregion

        //region Check điều kiện
        ResponseCheckConditionByConditionMapping responseCheckConditionByConditionMapping = this.getNextNodeByConditionMapping(CommandCheckConditionByConditionMapping.builder()
                .currentNode(currNode)
                .user(userEntity)
                .message(command.getMessage())
                .build());
        final String nextNodeId = responseCheckConditionByConditionMapping.getNextNodeId();
        final ResponseTrainingPredictFromAI responseTrainingPredictFromAI = responseCheckConditionByConditionMapping.getResponseTrainingPredictFromAI();

        // Không thỏa đk nào => Trả về wrongMessage
        if (StringUtils.isBlank(nextNodeId)) {
            //region Lưu message mà bot gửi đi
            if (BooleanUtils.isFalse(command.getIsTrying())) {
                kafkaTemplate.send(Constant.KafkaTopic.process_save_message_when_predict, objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(userEntity.getId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                        .nodeId(currNode.getNodeId())
                        .message(wrongMessage)
                        .from(EMessageHistoryFrom.BOT)
                        .entities(responseTrainingPredictFromAI != null ? responseTrainingPredictFromAI.getEntities() : null)
                        .checkAddMessageHistoryGroup(true)
                        .saveMessageEntityHistory(true)
                        .build()));
            }
            return ResponseTrainingPredict.builder()
                    .currentNodeId(currNode.getNodeId())
                    .message(wrongMessage)
                    .build();
            //endregion
        }

        // Node cuối
        if (nextNodeId.equals("_END")) {
            //region Lưu message mà bot gửi đi
            if (BooleanUtils.isFalse(command.getIsTrying())) {
                kafkaTemplate.send(Constant.KafkaTopic.process_save_message_when_predict, objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(userEntity.getId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                        .nodeId("_END")
                        .message(endMessage)
                        .from(EMessageHistoryFrom.BOT)
                        .entities(responseTrainingPredictFromAI != null ? responseTrainingPredictFromAI.getEntities() : null)
                        .checkAddMessageHistoryGroup(true)
                        .saveMessageEntityHistory(true)
                        .build()));
            }

            return ResponseTrainingPredict.builder()
                    .currentNodeId("_END")
                    .message(endMessage)
                    .build();
            //endregion
        }

        // Chuyển node tiếp theo
        NodeEntity nextNode = nodesByNodeId.get(nextNodeId);
        if (nextNode == null) {
            // Nếu không có node tiếp theo thì đây là node cuối cùng
            //region Lưu message mà bot gửi đi
            if (BooleanUtils.isFalse(command.getIsTrying())) {
                kafkaTemplate.send(Constant.KafkaTopic.process_save_message_when_predict, objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(userEntity.getId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                        .nodeId("_END")
                        .message(endMessage)
                        .from(EMessageHistoryFrom.BOT)
                        .entities(responseTrainingPredictFromAI != null ? responseTrainingPredictFromAI.getEntities() : null)
                        .checkAddMessageHistoryGroup(true)
                        .saveMessageEntityHistory(true)
                        .build()));
            }
            return ResponseTrainingPredict.builder()
                    .currentNodeId("_END")
                    .message(endMessage)
                    .build();
            //endregion
        }

        List<EntityEntity> redisEntities = this.updateRedisEntities(userEntity.getId(), command.getSessionId(), responseTrainingPredictFromAI != null ? responseTrainingPredictFromAI.getEntities() : null); // Cập nhật entities cho session này trên redis
        Map<String, String> variableMap = this.convertEntitiesToVariableMap(redisEntities);
        String returnMessage = this.variableMapping(variableMap, nextNode.getMessage());

        // Lưu message mà bot gửi đi
        if (BooleanUtils.isFalse(command.getIsTrying())) {
            kafkaTemplate.send(Constant.KafkaTopic.process_save_message_when_predict, objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(userEntity.getId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                    .nodeId(nextNodeId)
                    .message(returnMessage)
                    .from(EMessageHistoryFrom.BOT)
                    .entities(responseTrainingPredictFromAI != null ? responseTrainingPredictFromAI.getEntities() : null)
                    .checkAddMessageHistoryGroup(true)
                    .saveMessageEntityHistory(true)
                    .build()));
        }
        return ResponseTrainingPredict.builder()
                .currentNodeId(nextNodeId)
                .message(returnMessage)
                .build();
        //endregion
    }

    @Deprecated
    private UserEntity getUserForPredictSession(String secretKey) {
        List<UserEntity> userEntities = userService.getList(CommandGetListUser.builder()
                .secretKey(secretKey)
                .returnFields(List.of("id", "secret_key", "username"))
                .build());
        if (CollectionUtils.isEmpty(userEntities)) {
            return null;
        }
        return userEntities.get(0);
    }

    @Deprecated
    private ScriptEntity getScriptForPredictSession(String userId, String scriptId) {
        List<ScriptEntity> scripts = scriptService.getList(CommandGetListScript.builder()
                .userId(userId)
                .ids(List.of(scriptId))
                .hasNodes(true)
                .returnFields(List.of("id", "userId", "nodes", "wrong_message", "end_message"))
                .build());
        if (CollectionUtils.isEmpty(scripts)) {
            return null;
        }
        return scripts.get(0);
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

        String status = jedisService.get(userId + COLON + JedisService.PrefixRedisKey.trainingServerStatus);
        if (StringUtils.isBlank(status) || status.equals(ResponseTrainingServerStatus.EStatus.FREE.name().toLowerCase())) {
            return ResponseTrainingServerStatus.builder()
                    .status(ResponseTrainingServerStatus.EStatus.FREE)
                    .build();
        }

        return ResponseTrainingServerStatus.builder()
                .status(ResponseTrainingServerStatus.EStatus.BUSY)
                .build();
    }

    private List<EntityEntity> updateRedisEntities(String userId, String sessionId, List<EntityEntity> responseEntities) throws Exception {
        // Lấy entity từ redis
        List<EntityEntity> redisEntities = new ArrayList<>();
        String redisKey = Constant.JedisPrefix.userIdPrefix_ + userId + COLON +
                Constant.JedisPrefix.scriptSessionId_ + sessionId + COLON + "list_entities";
        String entitiesStr = jedisService.get(redisKey);
        if (StringUtils.isNotBlank(entitiesStr) &&
                CollectionUtils.isNotEmpty(redisEntities = objectMapper.readValue(entitiesStr, new TypeReference<List<EntityEntity>>() {
                }))) {
            // Xóa redis entity trùng với entity lấy từ predict response
            if (CollectionUtils.isNotEmpty(responseEntities)) {
                List<String> responseEntityTypeIds = responseEntities.stream().map(EntityEntity::getEntityTypeId).toList();
                List<EntityEntity> redisEntitiesNeedToRemove = redisEntities.stream().filter(e -> responseEntityTypeIds.contains(e.getEntityTypeId())).toList();
                redisEntities.removeAll(redisEntitiesNeedToRemove);
            }
        }
        if (CollectionUtils.isNotEmpty(responseEntities)) {
            redisEntities.addAll(responseEntities);
        }
        // Set lại lên redis
        jedisService.setWithExpired(redisKey, objectMapper.writeValueAsString(redisEntities), 60 * 60);

        // Trả về list redis entities
        return redisEntities;
    }

    private void setEntityTypeForListEntity(String userId, List<EntityEntity> entities) {
        if (StringUtils.isBlank(userId) || CollectionUtils.isEmpty(entities)) {
            return;
        }

        // Từ entity type id có trong mỗi entity => get ra được list entity type
        Map<String, EntityTypeEntity> entityTypeById = new HashMap<>();
        List<String> entityTypeIds = entities.stream().map(EntityEntity::getEntityTypeId).toList();
        if (CollectionUtils.isNotEmpty(entityTypeIds)) {
            List<EntityTypeEntity> entityTypeEntities = entityTypeService.getList(CommandGetListEntityType.builder()
                    .userId(userId)
                    .ids(entityTypeIds)
                    .build());
            if (CollectionUtils.isNotEmpty(entityTypeEntities)) {
                entityTypeEntities.forEach(entityTypeEntity -> entityTypeById.put(entityTypeEntity.getId(), entityTypeEntity));
            }
        }

        for (EntityEntity entity : entities) {
            entity.entityTypeMapping(entityTypeById);
        }
    }

    private Map<String, String> convertEntitiesToVariableMap(List<EntityEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return new HashMap<>();
        }

        Map<String, String> variableMap = new HashMap<>();
        for (EntityEntity entity : entities) {
            if (entity.getEntityType() == null) {
                continue;
            }
            variableMap.put(entity.getEntityType().getName(), entity.getValue()); // Ví dụ: <"Tên", "Nguyễn Văn A">
        }

        return variableMap;
    }

    private String variableMapping(Map<String, String> variableMap, String content) {
        for (String variableKey : variableMap.keySet()) {
            content = content.replace("{{" + variableKey + "}}", variableMap.get(variableKey));
        }
        return content.replaceAll("\\{\\{.*?\\}\\}", "");
    }

    private ResponseCheckConditionByConditionMapping getNextNodeByConditionMapping(CommandCheckConditionByConditionMapping command) {
        if (command.getCurrentNode() == null) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "condition_mappings_empty");
            return ResponseCheckConditionByConditionMapping.builder()
                    .nextNodeId(null)
                    .responseTrainingPredictFromAI(null)
                    .build();
        }

        // Nếu k có condition mapping nào thì trả về "_END"
        if (CollectionUtils.isEmpty(command.getCurrentNode().getConditionMappings())) {
            return ResponseCheckConditionByConditionMapping.builder()
                    .nextNodeId("_END")
                    .responseTrainingPredictFromAI(null)
                    .build();
        }

        // Nếu condition mapping không có next node nào cả thì trả về "_END"
        List<String> totalNextNodeIds = new ArrayList<>();
        command.getCurrentNode().getConditionMappings().forEach(cm -> {
            if (CollectionUtils.isNotEmpty(cm.getNext_node_ids())) {
                totalNextNodeIds.addAll(cm.getNext_node_ids());
            }
        });
        if (CollectionUtils.isEmpty(totalNextNodeIds)) {
            return ResponseCheckConditionByConditionMapping.builder()
                    .nextNodeId("_END")
                    .responseTrainingPredictFromAI(null)
                    .build();
        }

        // Check điều kiện của từng condition mapping
        List<String> intentIds = command.getCurrentNode().getConditionMappings().stream()
                .map(ConditionMappingEntity::getIntentId).toList();
        ResponseTrainingPredictFromAI responseTrainingPredictFromAI = null;
        for (ConditionMappingEntity conditionMapping : command.getCurrentNode().getConditionMappings()) {
            // Check đầu vào của condition mapping
            if (CollectionUtils.isEmpty(conditionMapping.getNext_node_ids())) {
                continue;
            }
            if (StringUtils.isBlank(conditionMapping.getIntentId()) &&
                    StringUtils.isBlank(conditionMapping.getKeyword()) &&
                    CollectionUtils.isEmpty(conditionMapping.getEntities())) {
                continue;
            }

            // Check keyword
            if (StringUtils.isNotBlank(conditionMapping.getKeyword())) {
                if (!command.getMessage().toLowerCase().contains(conditionMapping.getKeyword().toLowerCase())) {
                    continue; // Không thỏa mãn thì check condition mapping khác
                }
            }

            // Check intent
            if (StringUtils.isNotBlank(conditionMapping.getIntentId())) {
                try {
                    if (responseTrainingPredictFromAI == null) {
                        responseTrainingPredictFromAI = this.sendPredictRequest(CommandSendPredictRequest.builder()
                                .user(command.getUser())
                                .message(command.getMessage())
                                .intentIds(intentIds)
                                .build());
                        if (responseTrainingPredictFromAI == null) {
                            continue;
                        }

                        String intentId = responseTrainingPredictFromAI.getIntentId();
                        if (StringUtils.isBlank(intentId) || intentId.equals("-1")) {
                            continue;
                        }
                    }

                    if (!conditionMapping.getIntentId().equals(responseTrainingPredictFromAI.getIntentId())) {
                        continue;
                    }
                } catch (Exception e) {
                    log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
                    continue;
                }
            }

            // Check entity
//            if (CollectionUtils.isNotEmpty(conditionMapping.getEntities())) {
//                if (responseTrainingPredictFromAI == null) {
//                    responseTrainingPredictFromAI = this.sendPredictRequest(CommandSendPredictRequest.builder()
//                            .user(command.getUser())
//                            .message(command.getMessage())
//                            .intentIds(intentIds)
//                            .build());
//                    if (responseTrainingPredictFromAI == null) {
//                        continue;
//                    }
//                }
//
//                if (CollectionUtils.isEmpty(responseTrainingPredictFromAI.getEntities())) {
//                    continue;
//                }
//
//                boolean isEntityPass = true;
//                for (EntityEntity entity : conditionMapping.getEntities()) {
//                    EntityEntity responseEntity = responseTrainingPredictFromAI.getEntities().stream()
//                            .filter(e -> e.getEntityTypeId().equals(entity.getEntityTypeId())).findFirst().orElse(null);
//                    if (responseEntity == null) {
//                        isEntityPass = false;
//                        break;
//                    }
//                }
//                if (BooleanUtils.isFalse(isEntityPass)) {
//                    continue;
//                }
//            }

            if (responseTrainingPredictFromAI != null && CollectionUtils.isNotEmpty(responseTrainingPredictFromAI.getEntities())) {
                this.setEntityTypeForListEntity(command.getUser().getId(), responseTrainingPredictFromAI.getEntities()); // Khi predict từ training server về thì entity sẽ không có sẵn entity type mà chỉ có entity_type_id => Hàm này để set chi tiết entity_type cho từng entity
            }
            return ResponseCheckConditionByConditionMapping.builder()
                    .nextNodeId(conditionMapping.getNext_node_ids().get(0))
                    .responseTrainingPredictFromAI(responseTrainingPredictFromAI)
                    .build();
        }

        return ResponseCheckConditionByConditionMapping.builder()
                .nextNodeId(null)
                .responseTrainingPredictFromAI(null)
                .build();
    }

    private ResponseTrainingPredictFromAI sendPredictRequest(CommandSendPredictRequest command) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> commandRequest = new HashMap<>();
            commandRequest.put("text", command.getMessage());
            commandRequest.put("username", command.getUser().getUsername());
            commandRequest.put("user_id", command.getUser().getId());
            commandRequest.put("intent_ids", command.getIntentIds());

            HttpEntity<String> request =
                    new HttpEntity<>(objectMapper.writeValueAsString(commandRequest), headers);

            ResponseTrainingPredictFromAI responseTrainingPredictFromAI = restTemplate.postForObject(resourceBundle.getString("training.server") + "/predict", request, ResponseTrainingPredictFromAI.class);
            if (responseTrainingPredictFromAI == null) {
                return null;
            }

            if (CollectionUtils.isEmpty(responseTrainingPredictFromAI.getEntities())) {
                return responseTrainingPredictFromAI;
            }

            // Lấy entity value từ trong message bằng start và end pos
            for (EntityEntity responseEntity : responseTrainingPredictFromAI.getEntities()) {
                responseEntity.setValue(command.getMessage().substring(responseEntity.getStartPosition(), responseEntity.getEndPosition() + 1));
            }
            return responseTrainingPredictFromAI;
        } catch (Exception e) {
            log.error("[{}]: {}", e.getStackTrace()[0], e.getMessage());
            return null;
        }
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(T commandGetListBase) {
        return null;
    }

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {

    }
}
