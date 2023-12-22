package com.caovy2001.chatbot.service.training;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.*;
import com.caovy2001.chatbot.enumeration.EMessageHistoryFrom;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.command.CommandGetListIntent;
import com.caovy2001.chatbot.service.jedis.IJedisService;
import com.caovy2001.chatbot.service.jedis.JedisService;
import com.caovy2001.chatbot.service.kafka.KafkaConsumer;
import com.caovy2001.chatbot.service.message_history.IMessageHistoryService;
import com.caovy2001.chatbot.service.message_history.command.CommandAddMessageHistory;
import com.caovy2001.chatbot.service.node.INodeService;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandGetListPattern;
import com.caovy2001.chatbot.service.script.IScriptService;
import com.caovy2001.chatbot.service.script.command.CommandGetListScript;
import com.caovy2001.chatbot.service.training.command.*;
import com.caovy2001.chatbot.service.training.response.*;
import com.caovy2001.chatbot.service.training_history.ITrainingHistoryService;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistory;
import com.caovy2001.chatbot.service.training_history.command.CommandTrainingHistoryAdd;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistory;
import com.caovy2001.chatbot.service.training_history.response.ResponseTrainingHistoryAdd;
import com.caovy2001.chatbot.service.user.IUserService;
import com.caovy2001.chatbot.service.user.command.CommandGetListUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    @Autowired
    private KafkaConsumer kafkaConsumer;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private IPatternService patternService;

    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("application");

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
        command.setUserId(userEntity.getId());
        //endregion

        String chatSessionJedisKey = Constant.JedisPrefix.userIdPrefix_ + command.getUserId() +
                Constant.JedisPrefix.COLON +
                Constant.JedisPrefix.Pattern.chatHistorySessionIdPrefix_ + command.getSessionId();
        String chatHistoryFromRedisStr = this.jedisService.get(chatSessionJedisKey);
        List<Map<String, Object>> chatHistoryFromRedis =
                StringUtils.isNotBlank(chatHistoryFromRedisStr) ?
                        this.objectMapper.readValue(chatHistoryFromRedisStr, List.class) :
                        new ArrayList<>();
        Map<String, Object> chatHistoryFromRedisItem = new HashMap<>();

        //region Lấy script
        ScriptEntity script = this.getScriptForPredictSession(userEntity.getId(), command.getScriptId());
        if (script == null) {
            return this.returnException("script_null", ResponseTrainingPredict.class);
        }
        if (CollectionUtils.isEmpty(script.getNodes())) {
            return this.returnException("nodes_empty", ResponseTrainingPredict.class);
        }
        final String endMessage = script.getEndMessage();
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

        Map<String, Object> responseMessageMap = new HashMap<>();

        // Gửi message của client về socket topic ở khung chat của user
        Map<String, Object> clientMessageToUser = new HashMap<>();
        clientMessageToUser.put("current_node_id", command.getCurrentNodeId());
        clientMessageToUser.put("message", command.getMessage());
        messagingTemplate.convertAndSend(
                "/chat/" + command.getSessionId() + "/receive-from-client", clientMessageToUser);
        chatHistoryFromRedisItem = new HashMap<>();
        chatHistoryFromRedisItem.put("from", "USER");
        chatHistoryFromRedisItem.put("message", command.getMessage());
        chatHistoryFromRedis.add(chatHistoryFromRedisItem);
        this.jedisService.setWithExpired(chatSessionJedisKey, this.objectMapper.writeValueAsString(chatHistoryFromRedis), 30 * 60);

        //region Nếu đây là câu bắt đầu thì trả về message của node đầu tiên
        if ("_BEGIN".equals(command.getCurrentNodeId())) {
            // Lưu message mà bot gửi đi
            if (BooleanUtils.isFalse(command.getIsTrying())) {
                this.saveUserMessage(command, null);
                kafkaConsumer.processSaveMessageWhenPredictConsumer(objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(userEntity.getId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                        .nodeId(firstNode.getNodeId())
                        .message(firstNode.getMessage())
                        .from(EMessageHistoryFrom.BOT)
                        .checkAddMessageHistoryGroup(true)
                        .saveMessageEntityHistory(true)
                        .build()));
            }

            responseMessageMap.put("current_node_id", firstNode.getNodeId());
            responseMessageMap.put("message", firstNode.getMessage());
            messagingTemplate.convertAndSend(
                    "/chat/" + command.getSessionId() + "/receive-from-bot", responseMessageMap);
            chatHistoryFromRedisItem = new HashMap<>();
            chatHistoryFromRedisItem.put("from", "BOT");
            chatHistoryFromRedisItem.put("message", firstNode.getMessage());
            chatHistoryFromRedis.add(chatHistoryFromRedisItem);
            this.jedisService.setWithExpired(chatSessionJedisKey, this.objectMapper.writeValueAsString(chatHistoryFromRedis), 30 * 60);
            return ResponseTrainingPredict.builder().build();
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
                .sessionId(command.getSessionId())
                .build());
        final List<String> nextNodeIds = responseCheckConditionByConditionMapping.getNextNodeIds();
        final ResponseTrainingPredictFromAI responseTrainingPredictFromAI = responseCheckConditionByConditionMapping.getResponseTrainingPredictFromAI();
        this.saveUserMessage(command, responseTrainingPredictFromAI);

        // Không thỏa đk nào => Trả về wrongMessage
        if (nextNodeIds.size() == 1 && nextNodeIds.contains("-1")) {
            // Gửi tín hiệu đến socket topic của user để báo cho user biết có message out khỏi kịch bản
            Map<String, Object> clientMessageToUserToShowPopUp = new HashMap<>();
            clientMessageToUserToShowPopUp.put("current_node_id", command.getCurrentNodeId());
            clientMessageToUserToShowPopUp.put("session_id", command.getSessionId());
            clientMessageToUserToShowPopUp.put("script_id", command.getScriptId());
            messagingTemplate.convertAndSend(
                    "/chat-listener/" + command.getUserId(), clientMessageToUserToShowPopUp);
            chatHistoryFromRedisItem = new HashMap<>();
            chatHistoryFromRedisItem.put("from", "USER");
            chatHistoryFromRedisItem.put("message", command.getMessage());
            chatHistoryFromRedis.add(chatHistoryFromRedisItem);
            this.jedisService.setWithExpired(chatSessionJedisKey, this.objectMapper.writeValueAsString(chatHistoryFromRedis), 30 * 60);
            return ResponseTrainingPredict.builder().build();
        }

        List<EntityEntity> redisEntities = this.updateRedisEntities(userEntity.getId(), command.getSessionId(), responseTrainingPredictFromAI != null ? responseTrainingPredictFromAI.getEntities() : null); // Cập nhật entities cho session này trên redis
        Map<String, String> variableMap = this.convertEntitiesToVariableMap(redisEntities);
        List<String> returnMessages = new ArrayList<>();
        for (String nextNodeId : nextNodeIds) {
            if (nextNodeId.equals("-1")) {
                continue;
            }
            NodeEntity nextNode = nodesByNodeId.get(nextNodeId);
            String returnMessage = this.variableMapping(variableMap, nextNode.getMessage());
            returnMessages.add(returnMessage);
        }

        // Get response message from gpt
        String responseMessage = this.askGptToGetResponseMessage(returnMessages, userEntity.getId(), command.getSessionId());

        // Get acceptable next node ids
        List<String> acceptableNextNodeIds = new ArrayList<>();
        for (String nextNodeId: nextNodeIds) {
            if (nextNodeId.equals("-1")) {
                continue;
            }
            NodeEntity node = nodesByNodeId.get(nextNodeId);
            if (CollectionUtils.isNotEmpty(node.getConditionMappings())) {
                acceptableNextNodeIds.add(nextNodeId);
            }
        }

        // Lưu message mà bot gửi đi
        if (BooleanUtils.isFalse(command.getIsTrying())) {
            kafkaConsumer.processSaveMessageWhenPredictConsumer(objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(userEntity.getId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                    .nodeId(CollectionUtils.isNotEmpty(acceptableNextNodeIds)? acceptableNextNodeIds.get(0) :firstNode.getNodeId())
                    .message(responseMessage)
                    .from(EMessageHistoryFrom.BOT)
//                    .entities(responseTrainingPredictFromAI != null ? responseTrainingPredictFromAI.getEntities() : null)
                    .checkAddMessageHistoryGroup(true)
                    .saveMessageEntityHistory(true)
                    .build()));
        }

        responseMessageMap.put("current_node_id", CollectionUtils.isNotEmpty(acceptableNextNodeIds)? acceptableNextNodeIds.get(0) :firstNode.getNodeId());
        responseMessageMap.put("message", responseMessage);
        messagingTemplate.convertAndSend(
                "/chat/" + command.getSessionId() + "/receive-from-bot", responseMessageMap);
        chatHistoryFromRedisItem = new HashMap<>();
        chatHistoryFromRedisItem.put("from", "BOT");
        chatHistoryFromRedisItem.put("message", responseMessage);
        chatHistoryFromRedis.add(chatHistoryFromRedisItem);
        this.jedisService.setWithExpired(chatSessionJedisKey, this.objectMapper.writeValueAsString(chatHistoryFromRedis), 30 * 60);
        return ResponseTrainingPredict.builder().build();
        //endregion
    }

    private String askGptToGetResponseMessage(List<String> listMessages, String userId, String sessionId) throws Exception {
        String prompt = "Bạn là một Chatbot tuyệt vời và có thể hỗ trợ người dùng ở hầu hết các lĩnh vực. Bạn có khả năng trả lời người dùng một cách rất tự nhiên thông qua những thông tin được cung cấp. \n" +
                "Tôi có đoạn hội thoại sau:\n" +
                "CONVERSATION " +
                "Chatbot: ... \n" +
                "- Những thông tin hoặc câu trả lời mà Chatbot biết: \n" +
                "INFORMATION" +
                "- Hãy tổng hợp các thông tin trên thành một câu trả lời hoàn hảo và hợp với ngữ cảnh nhất cho Chatbot, những thông tin không liên quan hoặc không cần thiết thì có thể lược bỏ.\n" +
                "- Trả lời ngắn gọn, không lan man. \n" +
                "- Nếu người dùng có hỏi thì hãy đáp lại một cách tự nhiên.\n" +
                "- Nếu người dùng trả lời hoặc hỏi những thứ không liên quan, hãy trả lời lại một cách tự nhiên mà không cần phải dựa vào dữ liệu có sẵn.\n" +
                "- Trả lời theo mẫu sau và không cần phải giải thích gì thêm, chỉ trả lời 1 dòng duy nhất:\n" +
                "Chatbot: {{câu trả lời}}";

        // Generate CONVERSATION
        String chatSessionJedisKey = Constant.JedisPrefix.userIdPrefix_ + userId +
                Constant.JedisPrefix.COLON +
                Constant.JedisPrefix.Pattern.chatHistorySessionIdPrefix_ + sessionId;
        String chatHistoryFromRedisStr = this.jedisService.get(chatSessionJedisKey);
        List<Map<String, Object>> chatHistoryFromRedis =
                StringUtils.isNotBlank(chatHistoryFromRedisStr) ?
                        this.objectMapper.readValue(chatHistoryFromRedisStr, List.class) :
                        new ArrayList<>();
        if (CollectionUtils.isNotEmpty(chatHistoryFromRedis)) {
            String conversation = "";
            for (Map<String, Object> chatHistoryFromRedisItem : chatHistoryFromRedis) {
                if (((String) chatHistoryFromRedisItem.get("from")).equals("USER")) {
                    conversation += "User: " + chatHistoryFromRedisItem.get("message") + " \n";
                } else if (((String) chatHistoryFromRedisItem.get("from")).equals("BOT")) {
                    conversation += "Chatbot: " + chatHistoryFromRedisItem.get("message") + " \n";
                }
            }
            prompt = prompt.replace("CONVERSATION", conversation);
        } else {
            prompt = prompt.replace("CONVERSATION", "");
        }

        // Generate information
        String info = "";
        for (String message : listMessages) {
            info += "   + " + message + " \n";
        }
        prompt = prompt.replace("INFORMATION", info);

        // Get result
        String result = intentService.askGpt(prompt);
        result = result.replace("<br>", "");
        System.out.println(result);
        return result.split(":")[1].trim();
    }

    @Override
    public ResponseBase answerMessage(CommandTrainingAnswerMessage command) throws Exception {
        // Lưu message
        kafkaConsumer.processSaveMessageWhenPredictConsumer(objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(command.getUserId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                .nodeId(command.getCurrentNodeId())
                .message(command.getMessage())
                .from(EMessageHistoryFrom.BOT)
                .build()));

        // Bắn message về topic chung giữa client và user
        Map<String, Object> responseMessageMap = new HashMap<>();
        responseMessageMap.put("current_node_id", command.getCurrentNodeId());
        responseMessageMap.put("message", command.getMessage());
        messagingTemplate.convertAndSend(
                "/chat/" + command.getSessionId() + "/receive-from-bot", responseMessageMap);

        return ResponseTrainingAnswerMessage.builder().build();
    }

    private void saveUserMessage(CommandTrainingPredict command, ResponseTrainingPredictFromAI responseTrainingPredictFromAI) throws Exception {
        if (BooleanUtils.isFalse(command.getIsTrying())) {
//            kafkaTemplate.send(Constant.KafkaTopic.process_save_message_when_predict, objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(command.getUserId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
//                    .nodeId(command.getCurrentNodeId())
//                    .message(command.getMessage())
//                    .from(EMessageHistoryFrom.CUSTOMER)
//                    .checkAddMessageHistoryGroup(true)
//                    .entities(responseTrainingPredictFromAI != null ? responseTrainingPredictFromAI.getEntities() : null)
//                    .saveMessageEntityHistory(true)
//                    .build()));
            kafkaConsumer.processSaveMessageWhenPredictConsumer(objectMapper.writeValueAsString(CommandAddMessageHistory.builder().userId(command.getUserId()).sessionId(command.getSessionId()).scriptId(command.getScriptId())
                    .nodeId(command.getCurrentNodeId())
                    .message(command.getMessage())
                    .from(EMessageHistoryFrom.CUSTOMER)
                    .checkAddMessageHistoryGroup(true)
                    .entities(responseTrainingPredictFromAI != null ? responseTrainingPredictFromAI.getEntities() : null)
                    .saveMessageEntityHistory(true)
                    .build()));
        }
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
                    .build(), EntityTypeEntity.class);
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
                    .nextNodeIds(List.of("-1"))
                    .responseTrainingPredictFromAI(null)
                    .build();
        }

        // Nếu k có condition mapping nào thì trả về "_BEGIN"
//        if (CollectionUtils.isEmpty(command.getCurrentNode().getConditionMappings())) {
//            return ResponseCheckConditionByConditionMapping.builder()
//                    .nextNodeIds(List.of("_BEGIN"))
//                    .responseTrainingPredictFromAI(null)
//                    .build();
//        }

        // Nếu condition mapping không có next node nào cả thì trả về "_BEGIN"
//        List<String> totalNextNodeIds = new ArrayList<>();
//        command.getCurrentNode().getConditionMappings().forEach(cm -> {
//            if (CollectionUtils.isNotEmpty(cm.getNext_node_ids())) {
//                totalNextNodeIds.addAll(cm.getNext_node_ids());
//            }
//        });
//        if (CollectionUtils.isEmpty(totalNextNodeIds)) {
//            return ResponseCheckConditionByConditionMapping.builder()
//                    .nextNodeIds(List.of("_BEGIN"))
//                    .responseTrainingPredictFromAI(null)
//                    .build();
//        }

        ResponseTrainingPredictFromAI responseTrainingPredictFromAI = null;

        // Check intent
        List<String> intentIds = command.getCurrentNode().getConditionMappings().stream()
                .map(ConditionMappingEntity::getIntentId).toList();
        try {
            if (responseTrainingPredictFromAI == null) {
//                        responseTrainingPredictFromAI = this.sendPredictRequest(CommandSendPredictRequest.builder()
                responseTrainingPredictFromAI = this.askGptToGetIntents(CommandSendPredictRequest.builder()
                        .user(command.getUser())
                        .message(command.getMessage())
                        .intentIds(intentIds)
                        .sessionId(command.getSessionId())
                        .build());
            }
        } catch (Exception e) {
            log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
        }

        // Check điều kiện của từng condition mapping
        List<String> nextNodeIds = new ArrayList<>();
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

            if (StringUtils.isNotBlank(conditionMapping.getIntentId())) {
                if (responseTrainingPredictFromAI == null) {
                    continue;
                }

                if (responseTrainingPredictFromAI.getIntentIds().size() == 1 &&
                        responseTrainingPredictFromAI.getIntentIds().contains("-1")) {
                    nextNodeIds.add("-1");
                    continue;
                }

                if (!responseTrainingPredictFromAI.getIntentIds().contains(conditionMapping.getIntentId())) {
                    continue;
                }

            }

            if (responseTrainingPredictFromAI != null && CollectionUtils.isNotEmpty(responseTrainingPredictFromAI.getEntities())) {
                this.setEntityTypeForListEntity(command.getUser().getId(), responseTrainingPredictFromAI.getEntities()); // Khi predict từ training server về thì entity sẽ không có sẵn entity type mà chỉ có entity_type_id => Hàm này để set chi tiết entity_type cho từng entity
            }

            nextNodeIds.add(conditionMapping.getNext_node_ids().get(0));
        }

        return ResponseCheckConditionByConditionMapping.builder()
                .nextNodeIds(nextNodeIds)
                .responseTrainingPredictFromAI(responseTrainingPredictFromAI)
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

    private ResponseTrainingPredictFromAI askGptToGetIntents(CommandSendPredictRequest command) throws Exception {
        ResponseTrainingPredictFromAI responseTrainingPredictFromAI = ResponseTrainingPredictFromAI.builder().build();
        String message = "- Tôi có đoạn hội thoại sau:\n" +
                "CONVERSATION" +
                "- Tôi có những ví dụ về những intents có những pattern thuộc nó:\n" +
                "INTENT_WITH_PATTERN_EXAMPLES" +
                "- Câu nói cuối của User là \"CLIENT_PATTERN\". Hãy dự đoán xem nó thuộc những intent nào trong những intent sau: INTENT_NAMES | Không thuộc intent nào hoặc user nói bậy hoặc không muốn tiếp tục cuộc trò chuyện.\n" +
                "- Trả lời theo các tiêu chí sau:\n" +
                "   + Trả lời theo mẫu và không cần giải thích gì thêm: \n" +
                "{{số thứ tự}}: Intent {{mức độ chính xác từ 0-1 với 1 là cao nhất}} : {{tên intent}} | {{tên entity}}: {{giá trị của entity}} | {{tên entity}}: {{giá trị của entity}} | ...  \n" +
                "   + Chỉ trích xuất các entity có tên sau: ENTITY_NAMES. \n" +
                "   + Không trích xuất các entity không có tên với các entity tôi đã liệt kê. Giá trị của entity có phân biệt chữ thường và hoa.\n" +
                "   + Chỉ trích xuất các entity trong câu nói cuối của User.\n" +
                "   + Chỉ dự đoán intent của câu nói cuối của User.\n" +
                "   + Một câu có thể có nhiều intent. \n" +
                "   + Dự đoán ít nhất 3 intent. \n" +
                "   + Sau mỗi câu trả lời thì phải luôn xuống dòng bằng ký tự \\n. \n" +
                "   + Nếu không trích xuất được entity nào, trả lời theo mẫu sau:\n" +
                "{{số thứ tự}}: Intent {{mức độ chính xác từ 0-1 với 1 là cao nhất}}: {{tên intent}} | {{tên entity được liệt kê}}: không_có | {{tên entity được liệt kê}}: không_có | ... \n" +
                "   + Câu trả lời không chứa các dấu ngoặc nhọn, ngoặc đơn, ngoặc kép và dấu nháy kép\n" +
                "   + Hãy suy nghĩ từng bước thật kỹ trước khi trả lời.\n" +
                "   + Nếu câu trên của client không thuộc các intent trên hoặc client tỏ ý không muốn nói, không muốn trả lời hoặc trả lời sai, nó thuộc intent: Không thuộc intent nào hoặc user nói bậy hoặc không muốn tiếp tục cuộc trò chuyện.";

        // Generate CONVERSATION
        String chatSessionJedisKey = Constant.JedisPrefix.userIdPrefix_ + command.getUser().getId() +
                Constant.JedisPrefix.COLON +
                Constant.JedisPrefix.Pattern.chatHistorySessionIdPrefix_ + command.getSessionId();
        String chatHistoryFromRedisStr = this.jedisService.get(chatSessionJedisKey);
        List<Map<String, Object>> chatHistoryFromRedis =
                StringUtils.isNotBlank(chatHistoryFromRedisStr) ?
                        this.objectMapper.readValue(chatHistoryFromRedisStr, List.class) :
                        new ArrayList<>();
        if (CollectionUtils.isNotEmpty(chatHistoryFromRedis)) {
            String conversation = "";
            for (Map<String, Object> chatHistoryFromRedisItem : chatHistoryFromRedis) {
                if (((String) chatHistoryFromRedisItem.get("from")).equals("USER")) {
                    conversation += "   + User: " + chatHistoryFromRedisItem.get("message") + " \n";
                } else if (((String) chatHistoryFromRedisItem.get("from")).equals("BOT")) {
                    conversation += "   + Chatbot: " + chatHistoryFromRedisItem.get("message") + " \n";
                }
            }
            message = message.replace("CONVERSATION", conversation);
        } else {
            message = message.replace("CONVERSATION", "");
        }

        message = message.replace("CLIENT_PATTERN", command.getMessage());

        // Get all intent names
        List<IntentEntity> intents = intentService.getList(CommandGetListIntent.builder()
                .userId(command.getUser().getId())
                .ids(command.getIntentIds())
                .returnFields(List.of("id", "name"))
                .build(), IntentEntity.class);
        if (CollectionUtils.isEmpty(intents)) {
            return ResponseTrainingPredictFromAI.builder()
                    .intentIds(List.of("-1"))
                    .build();
        }
        List<String> intentNames = intents.stream().map(IntentEntity::getName).toList();
        message = message.replace("INTENT_NAMES", String.join(" | ", intentNames));

        // Add example patterns
        String examplePatterns = "";
        for (IntentEntity intent : intents) {
            Paginated<PatternEntity> patterns = patternService.getPaginatedList(CommandGetListPattern.builder()
                    .userId(command.getUser().getId())
                    .intentId(intent.getId())
                    .page(1)
                    .size(100)
                    .returnFields(List.of("id", "content"))
                    .build(), PatternEntity.class, CommandGetListPattern.class);
            if (CollectionUtils.isEmpty(patterns.getItems())) {
                continue;
            }

            examplePatterns += "   + " + intent.getName() + ": " + String.join(" | ", patterns.getItems().stream().map(PatternEntity::getContent).toList()) + "\n";
        }
        message = message.replace("INTENT_WITH_PATTERN_EXAMPLES", examplePatterns);

        // Get all entity types
        List<EntityTypeEntity> entityTypes = entityTypeService.getList(CommandGetListEntityType.builder()
                .userId(command.getUser().getId())
                .build(), EntityTypeEntity.class);
        if (CollectionUtils.isEmpty(entityTypes)) {
            message = message.replace("ENTITY_NAMES", "");
        } else {
            List<String> entityTypeNames = entityTypes.stream().map(EntityTypeEntity::getName).toList();
            message = message.replace("ENTITY_NAMES", String.join(" | ", entityTypeNames));
        }

        String results = intentService.askGpt(message);
        results = results.replace("<br>", "");
        System.out.println(results);
        //1: Intent 0.9: Nói tên | Tên: Vỹ | Tuổi: không_có | Địa chỉ: không_có \n
        //2: Intent 0.1: Không thuộc intent nào hoặc user nói bậy

        List<IntentEntity> resIntents = new ArrayList<>();
        for (String result : results.split("\n")) {
            result = result.replace("\\n", "");
            List<EntityEntity> entities = new ArrayList<>();
            if (result.split("\\|")[0].split(":").length < 3) {
                continue;
            }
            String resIntentStr = result.split("\\|")[0].split(":")[2].trim();
            if (result.split("\\|").length > 1) {
                for (int i = 1; i < result.split("\\|").length; i++) {
                    String resEntityStr = result.split("\\|")[i]; // Địa chỉ: Hải Phòng

                    if (resEntityStr.split(":").length <= 1 || resEntityStr.split(":")[1].contains("không_có")) {
                        continue;
                    }

                    String entityName = resEntityStr.split(":")[0].trim();
                    String entityValue = resEntityStr.split(":")[1].trim();
                    EntityTypeEntity entityType = entityTypes.stream().filter(entityTypeEntity -> entityTypeEntity.getName().equals(entityName)).findFirst().orElse(null);
                    if (entityType == null) {
                        continue;
                    }

                    if (command.getMessage().toLowerCase().contains(entityValue.toLowerCase())) {
                        EntityEntity entity = EntityEntity.builder()
                                .entityTypeId(entityType.getId())
                                .startPosition(command.getMessage().toLowerCase().indexOf(entityValue.toLowerCase()))
                                .endPosition(command.getMessage().toLowerCase().indexOf(entityValue.toLowerCase()) + entityValue.length() - 1)
                                .build();
                        entity.setValue(command.getMessage().substring(entity.getStartPosition(), entity.getEndPosition() + 1));
                        entities.add(entity);
                    }

                }
                responseTrainingPredictFromAI.setEntities(entities);
            }

            if (result.split("\\|")[0].split(":")[1].trim().split(" ").length < 2) {
                continue;
            }
            String intentAccuracy = result.split("\\|")[0].split(":")[1].trim().split(" ")[1];
            try {
                Float.parseFloat(intentAccuracy);
            } catch (Exception e) {
                continue;
            }

            if (Float.valueOf(intentAccuracy) < 0.79) {
                continue;
            }
            if (resIntents.size() == 3) {
                continue;
            }
            IntentEntity resIntentEntity = intents.stream().filter(intentEntity -> resIntentStr.trim().toLowerCase().contains(intentEntity.getName().trim().toLowerCase())).findFirst().orElse(null);
            if (resIntentEntity != null) {
                resIntents.add(resIntentEntity);
            }
            else if (resIntentStr.contains("Không thuộc intent nào")) {
                resIntents.add(IntentEntity.builder().id("-1").build());
            }
        }

        if (resIntents.size() > 0 && resIntents.get(0).getId().equals("-1")) {
            resIntents = new ArrayList<>();
            resIntents.add(IntentEntity.builder().id("-1").build());
        }

        responseTrainingPredictFromAI.setIntentIds(resIntents.stream().map(IntentEntity::getId).toList());
        return responseTrainingPredictFromAI;
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase) {
        return null;
    }

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {

    }
}
