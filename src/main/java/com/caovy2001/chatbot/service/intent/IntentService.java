package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.entity.ScriptIntentMappingEntity;
import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.repository.IntentRepository;
import com.caovy2001.chatbot.repository.es.IntentRepositoryES;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.intent.command.*;
import com.caovy2001.chatbot.service.intent.es.IIntentServiceES;
import com.caovy2001.chatbot.service.kafka.KafkaConsumer;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandGetListPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAddMany;
import com.caovy2001.chatbot.service.pattern.command.CommandProcessAfterCUDIntentPatternEntityEntityType;
import com.caovy2001.chatbot.service.script_intent_mapping.IScriptIntentMappingService;
import com.caovy2001.chatbot.service.script_intent_mapping.command.CommandGetListScriptIntentMapping;
import com.caovy2001.chatbot.utils.ChatbotStringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
        List<PatternEntity> patternsItem = new ArrayList<>();
        for (PatternEntity pattern : patterns) {
            if (intentPatternsMap.get(pattern.getIntentId()) != null) {
                patternsItem = intentPatternsMap.get(pattern.getIntentId());
            }
            patternsItem.add(pattern);
            intentPatternsMap.put(pattern.getIntentId(), patternsItem);

        }

        for (IntentEntity intent : intents) {
            if (intentPatternsMap.get(intent.getId()) != null) {
                intent.setPatterns(intentPatternsMap.get(intent.getId()));
            }
        }

//        for (IntentEntity intent : intents) {
//            if (BooleanUtils.isTrue(command.isHasPatterns())) {
//                List<PatternEntity> patterns = patternService.getList(CommandGetListPattern.builder()
//                        .userId(command.getUserId())
//                        .intentId(intent.getId())
//                        .hasEntities(command.isHasEntitiesOfPatterns())
//                        .hasEntityTypeOfEntities(command.isHasEntityTypesOfEntitiesOfPatterns())
//                        .build(), PatternEntity.class);
//                intent.setPatterns(patterns);
//            }
//        }
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
}
