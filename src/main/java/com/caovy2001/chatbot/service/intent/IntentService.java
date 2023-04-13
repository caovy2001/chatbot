package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.entity.ScriptIntentMappingEntity;
import com.caovy2001.chatbot.entity.es.IntentEntityES;
import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.repository.IntentRepository;
import com.caovy2001.chatbot.repository.PatternRepository;
import com.caovy2001.chatbot.repository.es.IntentRepositoryES;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.intent.command.*;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandGetListPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAdd;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IntentService extends BaseService implements IIntentService {

    @Autowired
    private IntentRepository intentRepository;

    @Autowired
    private IntentRepositoryES intentRepositoryES;

    @Autowired
    private PatternRepository patternRepository;

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

    @Override
    public ResponseIntentAdd add(CommandIntent command) {
        if (StringUtils.isAnyBlank(command.getCode(), command.getName(), command.getUserId())) {
            return returnException(ExceptionConstant.missing_param, ResponseIntentAdd.class);
        }

        IntentEntity existIntent = intentRepository.findByCodeAndUserId(command.getCode(), command.getUserId()).orElse(null);
        if (existIntent != null) {
            return returnException("intent_code_exist", ResponseIntentAdd.class);
        }

        IntentEntity intent = IntentEntity.builder()
                .code(command.getCode())
                .name(command.getName())
                .userId(command.getUserId())
                .createdDate(System.currentTimeMillis())
                .lastUpdatedDate(System.currentTimeMillis())
                .build();

        IntentEntity addedIntent = intentRepository.insert(intent);

        // Index ES
        this.indexES(CommandIndexingIntentES.builder()
                .userId(command.getUserId())
                .intents(List.of(intent))
                .doSetUserId(false)
                .build());

        return ResponseIntentAdd.builder()
                .id(addedIntent.getId())
                .build();
    }

    @Override
    public ResponseIntentAdd addMany(CommandIntentAddMany command) {
        if (CollectionUtils.isEmpty(command.getIntents()) || StringUtils.isBlank(command.getUserId())) {
            return this.returnException(ExceptionConstant.missing_param, ResponseIntentAdd.class);
        }
        ResponseIntentAdd responseIntentAdd = ResponseIntentAdd.builder()
                .ids(new ArrayList<>())
                .intents(new ArrayList<>())
                .build();
        List<IntentEntity> savedIntents = new ArrayList<>();
        for (IntentEntity intent : command.getIntents()) {
            IntentEntity existIntent = intentRepository.findByCodeAndUserId(intent.getCode(), command.getUserId()).orElse(null);
            if (existIntent != null) {
                intent.setId(existIntent.getId());
            } else {
                intent.setId(null);
            }

            intent.setUserId(command.getUserId());
            intent.setCreatedDate(System.currentTimeMillis());
            intent.setLastUpdatedDate(System.currentTimeMillis());
            IntentEntity savedIntent = intentRepository.save(intent);
            responseIntentAdd.getIds().add(savedIntent.getId());
            savedIntents.add(savedIntent);

            if (!CollectionUtils.isEmpty(intent.getPatterns())) {
                for (PatternEntity pattern : intent.getPatterns()) {
                    CommandPatternAdd commandPatternAdd = CommandPatternAdd.builder()
                            .userId(command.getUserId())
                            .content(pattern.getContent())
                            .intentId(savedIntent.getId())
                            .build();

                    patternService.add(commandPatternAdd);
                }
            }
        }

        if (BooleanUtils.isTrue(command.isReturnListDetails())) {
            responseIntentAdd.setIntents(savedIntents);
        }

        // Index ES
        this.indexES(CommandIndexingIntentES.builder()
                .userId(command.getUserId())
                .intents(savedIntents)
                .doSetUserId(false)
                .build());

        return responseIntentAdd;
    }

    @Override
    @Deprecated
    public ResponseIntents getByUserId(String userId) {
        if (StringUtils.isBlank(userId))
            return returnException(ExceptionConstant.missing_param, ResponseIntents.class);

        List<IntentEntity> intents = intentRepository.findByUserId(userId);
        List<PatternEntity> patterns = patternRepository.findByIntentIdInAndUserId(
                intents.stream().map(IntentEntity::getId).collect(Collectors.toList()),
                userId);

        for (IntentEntity intent : intents) {
            List<PatternEntity> patternsByIntent = patterns.stream()
                    .filter(patternEntity -> patternEntity.getIntentId().equals(intent.getId())).collect(Collectors.toList());
            intent.setPatterns(patternsByIntent);
        }

        return ResponseIntents.builder()
                .intents(intents)
                .build();
    }

    @Override
    public ResponseIntents getById(String id, String userId) {
        //find by user id
        if (id == null) {
            List<IntentEntity> intents = intentRepository.findByUserId(userId);
            List<PatternEntity> patterns = patternRepository.findByIntentIdInAndUserId(
                    intents.stream().map(IntentEntity::getId).collect(Collectors.toList()),
                    userId);

            for (IntentEntity intent : intents) {
                List<PatternEntity> patternsByIntent = patterns.stream()
                        .filter(patternEntity -> patternEntity.getIntentId().equals(intent.getId())).collect(Collectors.toList());
                intent.setPatterns(patternsByIntent);
            }

            return ResponseIntents.builder()
                    .intents(intents)
                    .build();
        }
        //find by intent id
        else {
            IntentEntity intent = intentRepository.findById(id).orElse(null);
            if (intent != null) {
                List<PatternEntity> patterns = patternRepository.findByIntentIdInAndUserId(
                        intent.getId(),
                        userId);
                intent.setPatterns(patterns);
            }

            return ResponseIntents.builder()
                    .intent(intent)
                    .build();
        }
    }

    @Override
    public ResponseIntents updateName(CommandIntent command, String userId) {
        if (command.getId() == null) {
            return returnException(ExceptionConstant.missing_param, ResponseIntents.class);
        }
        ResponseIntents intent = this.getById(command.getId(), userId);
        intent.getIntent().setName(command.getName());
        intent.getIntent().setLastUpdatedDate(System.currentTimeMillis());
        IntentEntity savedIntent = intentRepository.save(intent.getIntent());

        // Index ES
        this.indexES(CommandIndexingIntentES.builder()
                .userId(command.getUserId())
                .intents(List.of(savedIntent))
                .doSetUserId(false)
                .build());

        return ResponseIntents.builder().intent(intent.getIntent()).build();
    }

    @Override
    public ResponseIntents update(CommandIntent command) {
        if (StringUtils.isAnyBlank(command.getId(), command.getCode(), command.getUserId(), command.getName())) {
            return returnException(ExceptionConstant.missing_param, ResponseIntents.class);
        }

        IntentEntity intent = intentRepository.findById(command.getId()).orElse(null);
        if (intent == null) return returnException("intent_null", ResponseIntents.class);

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

        return ResponseIntents.builder()
                .intent(updatedIntent)
                .build();
    }

    @Override
    public ResponseIntents addPatterns(CommandIntentAddPattern command) {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getIntentId()) ||
                CollectionUtils.isEmpty(command.getPatterns())) {
            return returnException(ExceptionConstant.missing_param, ResponseIntents.class);
        }

        List<PatternEntity> patternsToAdd = new ArrayList<>();

        for (PatternEntity pattern : command.getPatterns()) {
            if (StringUtils.isBlank(pattern.getContent())) continue;

            pattern.setId(null);
            pattern.setIntentId(command.getIntentId());
            pattern.setUserId(command.getUserId());
            patternsToAdd.add(pattern);
        }

        List<PatternEntity> addedPatterns = patternService.addMany(patternsToAdd);

        if (CollectionUtils.isEmpty(addedPatterns)) {
            return returnException("add_patterns_fail", ResponseIntents.class);
        }

        return ResponseIntents.builder()
                .patterns(addedPatterns)
                .build();
    }

    @Override
    public Paginated<IntentEntity> getPaginationByUserId(String userId, int page, int size) {
        if (StringUtils.isBlank(userId)) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        long total = intentRepository.countByUserId(userId);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        List<IntentEntity> intents = intentRepository.findByUserId(userId, PageRequest.of(page, size));
        return new Paginated<>(intents, page, size, total);
    }

    @Override
    public Paginated<IntentEntity> getPagination(CommandGetListIntent command) {
        if (StringUtils.isBlank(command.getUserId())) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        if (command.getPage() <= 0 || command.getSize() <= 0) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        long total = mongoTemplate.count(query, IntentEntity.class);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        PageRequest pageRequest = PageRequest.of(command.getPage() - 1, command.getSize());
        query.with(pageRequest);
        List<IntentEntity> intentEntities = mongoTemplate.find(query, IntentEntity.class);
        this.setViewForListIntents(intentEntities, command);
        return new Paginated<>(intentEntities, command.getPage(), command.getSize(), total);
    }

    @Override
    public List<IntentEntity> addManyReturnList(@NonNull CommandIntentAddMany command) {
        if (StringUtils.isBlank(command.getUserId()) || CollectionUtils.isEmpty(command.getIntents())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        command.setReturnListDetails(true);
        ResponseIntentAdd responseIntentAdd = this.addMany(command);
        if (responseIntentAdd == null || CollectionUtils.isEmpty(responseIntentAdd.getIntents())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "cannot_add_many_intents");
            return null;
        }
        return responseIntentAdd.getIntents();
    }

    @Override
    public List<IntentEntity> getList(CommandGetListIntent command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return null;
        }

        List<IntentEntity> intents = mongoTemplate.find(query, IntentEntity.class);
        if (CollectionUtils.isEmpty(intents)) {
            return null;
        }

        this.setViewForListIntents(intents, command);
        return intents;
    }

    private void setViewForListIntents(List<IntentEntity> intents, CommandGetListIntent command) {
        if (BooleanUtils.isFalse(command.isHasPatterns())) {
            return;
        }

        for (IntentEntity intent : intents) {
            if (BooleanUtils.isTrue(command.isHasPatterns())) {
                List<PatternEntity> patterns = patternService.getList(CommandGetListPattern.builder()
                        .userId(command.getUserId())
                        .intentId(intent.getId())
                        .hasEntities(command.isHasEntitiesOfPatterns())
                        .hasEntityTypeOfEntities(command.isHasEntityTypesOfEntitiesOfPatterns())
                        .build());
                intent.setPatterns(patterns);
            }
        }
    }

    private Query buildQueryGetList(CommandGetListIntent command) {
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
    public ResponseIntents deleteIntent(String id, String userId) {
        if (id == null || userId == null) {
            return returnException(ExceptionConstant.missing_param, ResponseIntents.class);
        }
        intentRepository.deleteById(id);
        patternRepository.deleteByIntentIdAndUserId(id, userId);
        return ResponseIntents.builder().build();
    }

    private void indexES(CommandIndexingIntentES command) {
        try {
            // Đẩy vào kafka để index lên ES
            kafkaTemplate.send(Constant.KafkaTopic.process_indexing_intent_es, objectMapper.writeValueAsString(command));
        } catch (JsonProcessingException e) {
            log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage())? e.getMessage(): ExceptionConstant.error_occur);
        }
    }
}
