package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.*;
import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.repository.EntityRepository;
import com.caovy2001.chatbot.repository.IntentRepository;
import com.caovy2001.chatbot.repository.MessageHistoryRepository;
import com.caovy2001.chatbot.repository.es.IntentRepositoryES;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandEntityTypeAddMany;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandUpdateEntityType;
import com.caovy2001.chatbot.service.intent.command.*;
import com.caovy2001.chatbot.service.intent.es.IIntentServiceES;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAskGpt;
import com.caovy2001.chatbot.service.kafka.KafkaConsumer;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandGetListPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAddMany;
import com.caovy2001.chatbot.service.pattern.command.CommandProcessAfterCUDIntentPatternEntityEntityType;
import com.caovy2001.chatbot.service.script_intent_mapping.IScriptIntentMappingService;
import com.caovy2001.chatbot.service.script_intent_mapping.command.CommandGetListScriptIntentMapping;
import com.caovy2001.chatbot.service.training.response.ResponseTrainingPredictFromAI;
import com.caovy2001.chatbot.utils.ChatbotStringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class IntentService extends BaseService implements IIntentService {
    @Autowired
    private IntentRepository intentRepository;

    @Autowired
    private IntentRepositoryES intentRepositoryES;

    @Autowired
    private IPatternService patternService;

    @Autowired
    private IScriptIntentMappingService scriptIntentMappingService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private IIntentServiceES intentServiceES;

    @Autowired
    private KafkaConsumer kafkaConsumer;

    @Autowired
    private IEntityTypeService entityTypeService;

    @Autowired
    private MessageHistoryRepository messageHistoryRepository;

    @Override
    public <Entity extends BaseEntity, CommandAdd extends CommandAddBase> Entity add(CommandAdd commandAddBase) throws Exception {
        CommandIntentAdd command = (CommandIntentAdd) commandAddBase;

        if (StringUtils.isAnyBlank(command.getCode(), command.getName(), command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        if (CollectionUtils.isNotEmpty(this.getList(CommandGetListIntent.builder()
                .userId(command.getUserId())
                .code(command.getCode())
                .build(), IntentEntity.class))) {
            throw new Exception("intent_code_exist");
        }

        List<IntentEntity> addedIntents = this.add(CommandIntentAddMany.builder()
                .userId(command.getUserId())
                .intents(List.of(IntentEntity.builder()
                        .userId(command.getUserId())
                        .code(command.getCode())
                        .name(command.getName())
                        .build()))
                .build());

        if (CollectionUtils.isEmpty(addedIntents)) {
            throw new Exception(ExceptionConstant.error_occur);
        }

        return (Entity) addedIntents.get(0);
    }

    @Override
    public <Entity extends BaseEntity, CommandAddMany extends CommandAddManyBase> List<Entity> add(CommandAddMany commandAddManyBase) throws Exception {
        CommandIntentAddMany command = (CommandIntentAddMany) commandAddManyBase;

        if (CollectionUtils.isEmpty(command.getIntents()) || StringUtils.isBlank(command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        List<IntentEntity> intentsToSave = new ArrayList<>();
        List<PatternEntity> patternsToAdd = new ArrayList<>();
        List<String> intentCodes = command.getIntents().stream().map(IntentEntity::getCode).filter(StringUtils::isNotBlank).toList();
        if (CollectionUtils.isEmpty(intentCodes)) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        Map<String, IntentEntity> existIntentByCode = new HashMap<>();
        List<IntentEntity> listExistIntentsByCode = this.getList(CommandGetListIntent.builder()
                .userId(command.getUserId())
                .codes(intentCodes)
                .build(), IntentEntity.class);
        if (CollectionUtils.isNotEmpty(listExistIntentsByCode)) {
            listExistIntentsByCode.forEach(i -> existIntentByCode.put(i.getCode(), i));
        }

        for (IntentEntity intent : command.getIntents()) {
            if (StringUtils.isAnyBlank(intent.getName(), intent.getCode()) ||
                    existIntentByCode.get(intent.getCode()) != null) { // Check trùng code
                continue;
            }

            intent.setUserId(command.getUserId());
            intent.setCreatedDate(System.currentTimeMillis());
            intent.setLastUpdatedDate(System.currentTimeMillis());
            intentsToSave.add(intent);

            if (CollectionUtils.isNotEmpty(intent.getPatterns())) {
                for (PatternEntity pattern : intent.getPatterns()) {
                    pattern.setIntentCode(intent.getCode());
                    patternsToAdd.add(pattern);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(intentsToSave)) {
            intentsToSave = intentRepository.insert(intentsToSave);
        }
        if (BooleanUtils.isTrue(command.getReturnSameCodeIntent())) {
            intentsToSave.addAll(listExistIntentsByCode);
        }

        if (CollectionUtils.isEmpty(intentsToSave)) {
            throw new Exception(ExceptionConstant.error_occur);
        }

        // Save patterns
        if (CollectionUtils.isNotEmpty(patternsToAdd)) {
            Map<String, IntentEntity> intentByCode = new HashMap<>();
            intentsToSave.forEach(intentToSave -> {
                intentByCode.put(intentToSave.getCode(), intentToSave);
            });

            patternsToAdd.forEach(patternToAdd -> {
                patternToAdd.setIntentId(intentByCode.get(patternToAdd.getIntentCode()).getId());
            });

            patternService.add(CommandPatternAddMany.builder()
                    .userId(command.getUserId())
                    .patterns(patternsToAdd)
                    .build());
        }

        // Index ES
        this.indexES(CommandIndexingIntentES.builder()
                .userId(command.getUserId())
                .intents(intentsToSave)
                .doSetUserId(false)
                .build());

        // Xóa file Training_data.xlsx
//        kafkaTemplate.send(Constant.KafkaTopic.process_after_cud_intent_pattern_entity_entityType, objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
//                .userId(command.getUserId())
//                .build()));
        kafkaConsumer.processAfterCUDIntentPatternEntityEntityType(objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
                .userId(command.getUserId())
                .build()));

        return (List<Entity>) intentsToSave;
    }

    @Override
    public <Entity extends BaseEntity, CommandUpdate extends CommandUpdateBase> Entity update(CommandUpdate commandUpdateBase) throws Exception {
        CommandIntentUpdate command = (CommandIntentUpdate) commandUpdateBase;

        if (StringUtils.isAnyBlank(command.getId(), command.getCode(), command.getUserId(), command.getName())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        List<IntentEntity> intents = this.getList(CommandGetListIntent.builder()
                .userId(command.getUserId())
                .ids(List.of(command.getId()))
                .build(), IntentEntity.class);
        if (CollectionUtils.isEmpty(intents)) {
            throw new Exception("intent_null");
        }

        IntentEntity intent = intents.get(0);
        intent.setCode(command.getCode());
        intent.setName(command.getName());
        intent.setLastUpdatedDate(System.currentTimeMillis());
        IntentEntity updatedIntent = intentRepository.save(intent);

        // Index ES
        this.indexES(CommandIndexingIntentES.builder()
                .userId(command.getUserId())
                .intents(List.of(updatedIntent))
                .doSetUserId(false)
                .build());

        // Xóa file Training_data.xlsx
//        kafkaTemplate.send(Constant.KafkaTopic.process_after_cud_intent_pattern_entity_entityType, objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
//                .userId(command.getUserId())
//                .build()));
        kafkaConsumer.processAfterCUDIntentPatternEntityEntityType(objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
                .userId(command.getUserId())
                .build()));

        return (Entity) updatedIntent;
    }

    @Override
    public <CommandGetList extends CommandGetListBase> boolean delete(CommandGetList commandGetListBase) throws Exception {
        CommandGetListIntent command = (CommandGetListIntent) commandGetListBase;

        if (StringUtils.isBlank(command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        // Quyết định những trường trả về
        command.setReturnFields(List.of("id"));

        List<IntentEntity> intents = this.getList(command, IntentEntity.class);
        if (CollectionUtils.isEmpty(intents)) {
            return false;
        }

        List<String> intentIds = intents.stream().map(IntentEntity::getId).toList();
        if (CollectionUtils.isEmpty(intentIds)) {
            return false;
        }

        boolean result = intentRepository.deleteAllByIdIn(intentIds) > 0;

        if (BooleanUtils.isTrue(result)) {
            if (command.isHasPatterns()) {
                patternService.delete(CommandGetListPattern.builder()
                        .userId(command.getUserId())
                        .intentIds(intentIds)
                        .hasEntities(true)
                        .build());
            }

            // Remove ES
            CompletableFuture.runAsync(() -> {
                try {
                    intentServiceES.delete(CommandDeleteIntentES.builder()
                            .ids(intentIds)
                            .build());
                } catch (Exception e) {
                    log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
                }
            });

            // Xóa file Training_data.xlsx
//            kafkaTemplate.send(Constant.KafkaTopic.process_after_cud_intent_pattern_entity_entityType, objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
//                    .userId(command.getUserId())
//                    .build()));
            kafkaConsumer.processAfterCUDIntentPatternEntityEntityType(objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
                    .userId(command.getUserId())
                    .build()));
        }

        return result;
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase) {
        CommandGetListIntent command = (CommandGetListIntent) commandGetListBase;

        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();

        andCriteriaList.add(Criteria.where("user_id").is(command.getUserId()));

        if (StringUtils.isNotBlank(command.getKeyword())) {
            orCriteriaList.add(Criteria.where("name").regex(ChatbotStringUtils.stripAccents(command.getKeyword().trim())));
            orCriteriaList.add(Criteria.where("code").regex(ChatbotStringUtils.stripAccents(command.getKeyword().trim().toLowerCase())));
        }

        if (CollectionUtils.isNotEmpty(command.getIds())) {
            andCriteriaList.add(Criteria.where("id").in(command.getIds()));
        }

        if (StringUtils.isNotBlank(command.getCode())) {
            andCriteriaList.add(Criteria.where("code").is(command.getCode()));
        }

        if (CollectionUtils.isNotEmpty(command.getCodes())) {
            andCriteriaList.add(Criteria.where("code").in(command.getCodes()));
        }

        if (CollectionUtils.isNotEmpty(command.getScriptIds())) {
            List<ScriptIntentMappingEntity> scriptIntentMappingEntities = scriptIntentMappingService.getList(CommandGetListScriptIntentMapping.builder()
                    .userId(command.getUserId())
                    .scriptIds(command.getScriptIds())
                    .returnFields(List.of("intent_id"))
                    .build());
            if (CollectionUtils.isEmpty(scriptIntentMappingEntities)) {
                return null;
            } else {
                andCriteriaList.add(Criteria.where("id").in(
                        scriptIntentMappingEntities.stream().map(ScriptIntentMappingEntity::getIntentId).filter(StringUtils::isNotBlank).toList()
                ));
            }
        }

        if (CollectionUtils.isNotEmpty(command.getDateFilters())) {
            for (DateFilter dateFilter : command.getDateFilters()) {
                if (dateFilter.getFromDate() != null &&
                        dateFilter.getToDate() != null &&
                        StringUtils.isNotBlank(dateFilter.getFieldName())) {
                    andCriteriaList.add(Criteria.where(dateFilter.getFieldName()).gte(dateFilter.getFromDate()).lte(dateFilter.getToDate()));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList);
        }
        if (CollectionUtils.isNotEmpty(andCriteriaList)) {
            criteria.andOperator(andCriteriaList);
        }

        query.addCriteria(criteria);
        if (CollectionUtils.isNotEmpty(command.getReturnFields())) {
            query.fields().include(Arrays.copyOf(command.getReturnFields().toArray(), command.getReturnFields().size(), String[].class));
        }
        return query;
    }

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {
        List<IntentEntity> intents = (List<IntentEntity>) entitiesBase;
        CommandGetListIntent command = (CommandGetListIntent) commandGetListBase;

        if (BooleanUtils.isFalse(command.isHasPatterns())) {
            return;
        }

        // Set pattern
        List<String> intentIds = intents.stream().map(IntentEntity::getId).toList();
        List<PatternEntity> patterns = patternService.getList(CommandGetListPattern.builder()
                .userId(command.getUserId())
                .intentIds(intentIds)
                .hasEntities(command.isHasEntitiesOfPatterns())
                .hasEntityTypeOfEntities(command.isHasEntityTypesOfEntitiesOfPatterns())
                .build(), PatternEntity.class);

        Map<String, List<PatternEntity>> intentPatternsMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(patterns)) {
            List<PatternEntity> patternsItem = new ArrayList<>();
            for (PatternEntity pattern : patterns) {
                if (intentPatternsMap.get(pattern.getIntentId()) != null) {
                    patternsItem = intentPatternsMap.get(pattern.getIntentId());
                } else {
                    patternsItem = new ArrayList<>();
                }
                patternsItem.add(pattern);
                intentPatternsMap.put(pattern.getIntentId(), patternsItem);

            }
        }

        for (IntentEntity intent : intents) {
            if (intentPatternsMap.get(intent.getId()) != null) {
                intent.setPatterns(intentPatternsMap.get(intent.getId()));
            }
        }
    }

    private void indexES(CommandIndexingIntentES command) {
        try {
            // Đẩy vào kafka để index lên ES
//            kafkaTemplate.send(Constant.KafkaTopic.process_indexing_intent_es, objectMapper.writeValueAsString(command));
            kafkaConsumer.processIndexingIntentES(objectMapper.writeValueAsString(command));
        } catch (IOException e) {
            log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
        }
    }

    @Override
    public Boolean suggestPattern(CommandIntentSuggestPattern command) throws Exception {
        if (StringUtils.isAnyBlank(command.getIntentId(), command.getUserId(), command.getExamplePattern()) || command.getNumOfPatterns() <= 0) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        // Tìm intent theo intentId và userId
        List<IntentEntity> intents = this.getList(CommandGetListIntent.builder()
                .userId(command.getUserId())
                .ids(List.of(command.getIntentId()))
                .build(), IntentEntity.class);
        if (CollectionUtils.isEmpty(intents)) {
            throw new Exception(ExceptionConstant.Intent.intent_not_found);
        }
        IntentEntity intent = intents.get(0);

        List<EntityTypeEntity> allEntityTypes = entityTypeService.getList(CommandGetListEntityType.builder()
                .userId(command.getUserId())
                .build(), EntityTypeEntity.class);
        List<String> allEntityTypeNames = null;
        if (CollectionUtils.isNotEmpty(allEntityTypes)) {
            allEntityTypeNames = allEntityTypes.stream().map(EntityTypeEntity::getName).toList();
        }

        // Generate message
        String message = "Liệt kê NUM_OF_PATTERNS patterns thuộc intent \"INTENT_NAME\".\nví dụ: \"1. EXAMPLE_PATTERN\"\nTrả lời theo các tiêu chí sau:\n- Trả lời theo mẫu sau và không cần giải thích gì thêm:\n+ {{số thứ tự}}: {{giá trị của pattern}} | {{entity1}}: {{giá trị của entity1}} | {{entity2}}: {{giá trị của entity2}} | ...\n- Câu trả lời không chứa các ký tự đặc biệt như ngoặc đơn, ngoặc kép, ngoặc nhọn. Không chứa dấu chấm ở cuối câu. \n- Trích xuất các entityALL_ENTITY_TYPE_NAMES. Hạn chế tạo ra entity mới với ý nghĩa tương đồng. Nếu không có các entity này thì có thể tạo ra entity mới.\n- Giá trị các entity trong câu có thể lấy random và gán thẳng giá trị của nó vào trong câu, không để dấu ngoặc nhọn như {{aa}}, có phân biệt chữ hoa và chữ thường, {{giá trị entity}} phải lấy từ trong câu, ví dụ: \"1: Tôi tên là Minh và tôi 21 tuổi | Tên: Minh | Tuổi: 21 | ... \".\n- Nếu không trích xuất được entity thì chỉ cần trả lời như sau:\n+ {{số thứ tự}}: {{giá trị của pattern}}\n- Làm như sau là sai: \"Tôi là {{tên}} | Tên: {{tên}}\", \"Bạn sống ở đâu? | Địa chỉ: {{địa chỉ}}\", \"Tôi là {{Hùng}} | Tên: Hùng\".";
//        String message = "liệt kê NUM_OF_PATTERNS patterns thuộc intent \"INTENT_NAME\".\nví dụ: \"1: EXAMPLE_PATTERN\"\ntrả lời theo mẫu sau; không cần giải thích gì thêm; không chứa các ký tự đặc biệt như ngoặc đơn hoặc ngoặc kép; có thể lấy giá trị random cho các entity trong câu:\n- {{số thứ tự}}: {{giá trị của pattern}} | {{entity1}}: {{giá trị của entity1}} | {{entity2}}: {{giá trị của entity2}} | ...";
        message = message.replace("NUM_OF_PATTERNS", command.getNumOfPatterns().toString());
        message = message.replace("INTENT_NAME", intent.getName());
        message = message.replace("EXAMPLE_PATTERN", command.getExamplePattern());
        if (CollectionUtils.isNotEmpty(allEntityTypes)) {
            message = message.replace("ALL_ENTITY_TYPE_NAMES", ": " + String.join(", ", allEntityTypeNames));
        } else {
            message = message.replace("ALL_ENTITY_TYPE_NAMES", "");
        }
        System.out.println(message);

        // Send request sang api chat gpt và nhận được response:
//        String response = "1: Tôi là Minh, 25 tuổi đấy | Tên: Minh | Tuổi: 25\n2: Chào bạn, mình là Hương, 30 tuổi đây | Tên: Hương | Tuổi: 30\n3: Mình tên là Đức, mình 28 tuổi nhé | Tên: Đức | Tuổi: 28 <br>";
        String response = this.askGpt(message);
        if (StringUtils.isBlank(response)) {
            throw new Exception(ExceptionConstant.error_occur);
        }
        response = response.replace("<br>", "");

        String[] patternStrs = response.split("\n");
        List<PatternEntity> patternEntities = new ArrayList<>();
        List<EntityEntity> entityEntities = new ArrayList<>();
        for (String patternStr : patternStrs) {
            String pattern = null;
            if (patternStr.contains("|")) {
                pattern = patternStr.split("\\|")[0].split(":")[1];
            } else {
                pattern = new String(patternStr);
            }
            pattern = pattern.trim();
            PatternEntity patternEntity = PatternEntity.builder()
                    .userId(command.getUserId())
                    .content(pattern)
                    .intentId(command.getIntentId())
                    .uuid(UUID.randomUUID().toString())
                    .build();
            patternEntities.add(patternEntity);

            // Lấy entity
            Map<String, EntityTypeEntity> entityTypeByName = new HashMap<>();
            if (patternStr.contains("|") && patternStr.split("\\|").length > 1 && StringUtils.isNotBlank(patternStr.split("\\|")[1].trim())) {
                for (int i = 1; i < patternStr.split("\\|").length; i++) {
                    String entityStr = patternStr.split("\\|")[i];
                    if (!entityStr.contains(":") || entityStr.split(":").length <= 1) {
                        continue;
                    }

                    entityStr = entityStr.trim();
                    String entityTypeName = entityStr.split(":")[0];
                    entityTypeName = entityTypeName.trim();
                    String entityValue = entityStr.split(":")[1];
                    entityValue = entityValue.trim();
                    if (StringUtils.isBlank(entityValue)) {
                        continue;
                    }

                    EntityTypeEntity entityType = entityTypeByName.get(entityTypeName);
                    if (entityType == null) {
                        // Tìm entity type theo tên
                        List<EntityTypeEntity> existEntityTypesByLowerCaseName = entityTypeService.getList(CommandGetListEntityType.builder()
                                .userId(command.getUserId())
                                .lowerCaseName(entityTypeName.trim().toLowerCase())
                                .returnFields(List.of("id", "lower_case_name"))
                                .build(), EntityTypeEntity.class);
                        if (CollectionUtils.isNotEmpty(existEntityTypesByLowerCaseName)) {
                            entityType = existEntityTypesByLowerCaseName.get(0);
                        } else {
                            List<EntityTypeEntity> savedEntityTypes = entityTypeService.add(CommandEntityTypeAddMany.builder()
                                    .userId(command.getUserId())
                                    .entityTypes(List.of(EntityTypeEntity.builder()
                                            .userId(command.getUserId())
                                            .uuid(UUID.randomUUID().toString())
                                            .name(entityTypeName)
                                            .build()))
                                    .build());
                            entityType = savedEntityTypes.get(0);
                        }
                        entityTypeByName.put(entityTypeName, entityType);
                    }

                    int startPos = patternEntity.getContent().indexOf(entityValue);
                    if (startPos == -1) {
                        continue;
                    }
                    int endPos = startPos + entityValue.length() - 1;
                    EntityEntity entityEntity = EntityEntity.builder()
                            .userId(command.getUserId())
                            .value(entityValue)
                            .patternUuid(patternEntity.getUuid())
                            .entityTypeId(entityTypeByName.get(entityTypeName).getId())
                            .startPosition(startPos)
                            .endPosition(endPos)
                            .build();
                    entityEntities.add(entityEntity);
                }
            }
        }

        // Group entity type
        this.groupEntityType(command.getUserId(), command.getIntentId());

        // Lưu pattern và entity xuống db
        patternService.add(CommandPatternAddMany.builder()
                .userId(command.getUserId())
                .patterns(patternEntities)
                .commandEntityAddMany(CommandEntityAddMany.builder()
                        .userId(command.getUserId())
                        .entities(entityEntities)
                        .build())
                .build());
        return true;
    }

    public String askGpt(String message) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> commandRequest = new HashMap<>();
            commandRequest.put("message", message);

            HttpEntity<String> request =
                    new HttpEntity<>(objectMapper.writeValueAsString(commandRequest), headers);

            ResponseIntentAskGpt responseIntentAskGpt = restTemplate.postForObject("https://f1d0-113-185-74-57.ngrok-free.app/api/ask", request, ResponseIntentAskGpt.class);
            if (responseIntentAskGpt == null) {
                return null;
            }

            return responseIntentAskGpt.getResult();
        } catch (Exception e) {
            log.error("[{}]: {}", e.getStackTrace()[0], e.getMessage());
            return null;
        }
    }

    public void groupEntityType(String userId, String intentId) throws Exception {
        List<EntityTypeEntity> allEntityTypes = entityTypeService.getList(CommandGetListEntityType.builder()
                .userId(userId)
                .intentId(intentId)
                .build(), EntityTypeEntity.class);
        if (CollectionUtils.isEmpty(allEntityTypes) || allEntityTypes.size() <= 3) {
            return;
        }

        List<String> allEntityTypeNames = allEntityTypes.stream().map(EntityTypeEntity::getName).toList();
        Map<String, EntityTypeEntity> entityTypeMapByName = new HashMap<>();
        allEntityTypes.forEach(entityType -> {
            entityTypeMapByName.put(entityType.getName().toLowerCase().trim(), entityType);
        });

        Map<String, EntityTypeEntity> entityTypeMapById = new HashMap<>();
        allEntityTypes.forEach(entityType -> {
            entityTypeMapById.put(entityType.getId(), entityType);
        });

        Map<String, EntityTypeEntity> entityTypeAfterMapById = new HashMap<>();

        String message = "Tôi có các tên entity: ENTITY_TYPE_NAMES. Gộp các tên entity gần đồng nghĩa lại với nhau.\nTrả lời theo các tiêu chí sau:\n- Không cần giải thích gì và chỉ cần trả lời theo mẫu sau: \"{{số thứ tự}}: {{tên entity}}, {{tên enitty}}, ... | {{tên entity mới}}\". \n- Các tên entity chỉ lấy ở các tên entity mà tôi đã đưa, {{tên entity mới}} thì có thể tạo ra tên mới có ý nghĩa bao quát các tên entity được gộp, nếu chỉ có 1 entity được gộp thì giữ lại tên của entity đó.\n- Mỗi tên entity chỉ được gộp một lần.\n- Không chứa các dấu ngoặc nhọn, ngoặc đơn, ngoặc kép.";
        message = message.replace("ENTITY_TYPE_NAMES", String.join(", ", allEntityTypeNames));
        String resultFromGpt = this.askGpt(message);
        resultFromGpt = resultFromGpt.replace("<br>", "");
//        String resultFromGpt = "1: Tên, Địa chỉ, Đường, Số nhà, Quê | Địa chỉ\n2: Thành phố, Khu vực, Quận, Tỉnh | Địa phương\n3: Cư trú, Thị trấn, Phường | Địa bàn\n4: Sinh ra | Ngày sinh <br>";
//        System.out.println(result);

        List<String> results = List.of(resultFromGpt.split("\\n"));
        Map<String, String> entityNameHistoryById = new HashMap<>();
        Map<String, String> entityTypeAfterById = new HashMap<>();
        for (String result : results) {
            List<String> entityNamesBefore = List.of(result.split("\\|")[0].trim().split(": ")[1].trim().split(", "));
            String entityNameAfter = result.split("\\|")[1].trim();
            EntityTypeEntity entityTypeAfter = entityTypeMapByName.get(entityNameAfter.toLowerCase().trim());
            if (entityTypeAfter == null) {
                List<EntityTypeEntity> savedEntityTypes = entityTypeService.add(CommandEntityTypeAddMany.builder()
                        .userId(userId)
                        .entityTypes(List.of(EntityTypeEntity.builder()
                                .userId(userId)
                                .uuid(UUID.randomUUID().toString())
                                .name(entityNameAfter)
                                .build()))
                        .build());
                entityTypeAfter = savedEntityTypes.get(0);
            }
            entityTypeAfterMapById.put(entityTypeAfter.getId(), entityTypeAfter);

            for (String entityNameBefore : entityNamesBefore) {
                EntityTypeEntity entityType = entityTypeMapByName.get(entityNameBefore.trim().toLowerCase());
                if (entityType != null && !entityType.getId().equals(entityTypeAfter.getId())) {
                    entityNameHistoryById.put(entityType.getId(), entityTypeAfter.getId());
                }
            }
        }

        // Update
        List<MessageHistoryEntity> messageHistoryEntitiesToUpdate = new ArrayList<>();
        for (String oldEntityTypeId : entityNameHistoryById.keySet()) {
            String newEntityTypeId = entityNameHistoryById.get(oldEntityTypeId);
            // Update many entity/message entity history
            Criteria criteria = Criteria.where("entity_type_id").is(oldEntityTypeId);
            Query queryUpdate1 = new Query(criteria);

            Update update = new Update();
            update.set("entity_type_id", newEntityTypeId);

            mongoTemplate.updateMulti(queryUpdate1, update, EntityEntity.class);
            mongoTemplate.updateMulti(queryUpdate1, update, MessageEntityHistoryEntity.class);

            // Update message history
            Criteria criteria2 = Criteria.where("entities").elemMatch(Criteria.where("entity_type_id").regex("(?i)" + oldEntityTypeId));
            Query queryUpdate2 = new Query(criteria2);
            List<MessageHistoryEntity> messageHistoryEntities = mongoTemplate.find(queryUpdate2, MessageHistoryEntity.class);
            for (MessageHistoryEntity messageHistoryEntity : messageHistoryEntities) {
                for (Document docEntity : messageHistoryEntity.getEntities()) {
                    docEntity.put("entity_type_id", newEntityTypeId);
                    Document docEntityType = (Document) docEntity.get("entity_type");
                    docEntityType.put("id", newEntityTypeId);
                    docEntityType.put("name", entityTypeMapById.get(newEntityTypeId).getName());
                    docEntity.put("entity_type", docEntityType);
                }
            }
            messageHistoryEntitiesToUpdate.addAll(messageHistoryEntities);

            // Update is_hided of entity types
            Criteria criteria3 = Criteria.where("id").is(oldEntityTypeId);
            Query queryUpdate3 = new Query(criteria3);

            Update update3 = new Update();
            update.set("is_hided", true);

            mongoTemplate.updateMulti(queryUpdate3, update3, EntityTypeEntity.class);

        }
        messageHistoryRepository.saveAll(messageHistoryEntitiesToUpdate);
    }

}
