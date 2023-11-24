package com.caovy2001.chatbot.service.entity;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.repository.EntityRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.kafka.KafkaConsumer;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandGetListPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandProcessAfterCUDIntentPatternEntityEntityType;
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

import java.util.*;

@Service
@Slf4j
public class EntityService extends BaseService implements IEntityService {
    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IEntityTypeService entityTypeService;

    @Autowired
    private IPatternService patternService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaConsumer kafkaConsumer;

    @Override
    public <Entity extends BaseEntity, CommandAddMany extends CommandAddManyBase> List<Entity> add(CommandAddMany commandAddManyBase) throws Exception {
        CommandEntityAddMany command = (CommandEntityAddMany) commandAddManyBase;

        if (StringUtils.isBlank(command.getUserId()) || CollectionUtils.isEmpty(command.getEntities())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        List<EntityEntity> entitiesToAdd = new ArrayList<>();
        for (EntityEntity entity : command.getEntities()) {
            if (StringUtils.isAnyBlank(entity.getPatternId(), entity.getEntityTypeId(), entity.getValue())) {
                log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
                continue;
            }

            if (entity.getStartPosition() < 0 ||
                    entity.getEndPosition() < 0 ||
                    entity.getStartPosition() > entity.getEndPosition()) {
                log.error("[{}]: {}", new Exception().getStackTrace()[0], "invalid_start_and_end_position");
                continue;
            }

            entitiesToAdd.add(EntityEntity.builder()
                    .userId(command.getUserId())
                    .patternId(entity.getPatternId())
                    .entityTypeId(entity.getEntityTypeId())
                    .value(entity.getValue())
                    .startPosition(entity.getStartPosition())
                    .endPosition(entity.getEndPosition())
                    .build());
        }

        if (CollectionUtils.isEmpty(entitiesToAdd)) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "entity_to_save_empty");
            return null;
        }

        List<EntityEntity> entities = entityRepository.insert(entitiesToAdd);
        if (CollectionUtils.isEmpty(entities)) {
            throw new Exception(ExceptionConstant.error_occur);
        }

        // Xóa file Training_data.xlsx
//        kafkaTemplate.send(Constant.KafkaTopic.process_after_cud_intent_pattern_entity_entityType, objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
//                .userId(command.getUserId())
//                .build()));
        kafkaConsumer.processAfterCUDIntentPatternEntityEntityType(objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
                .userId(command.getUserId())
                .build()));

        return (List<Entity>) entities;
    }

    @Override
    public <CommandGetList extends CommandGetListBase> boolean delete(CommandGetList commandGetListBase) throws Exception {
        CommandGetListEntity command = (CommandGetListEntity) commandGetListBase;

        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return false;
        }

        // Quyết định những trường trả về
        command.setReturnFields(List.of("id"));

        List<EntityEntity> entities = this.getList(command, EntityEntity.class);
        if (CollectionUtils.isEmpty(entities)) {
            return false;
        }

        List<String> entityIds = entities.stream().map(EntityEntity::getId).toList();
        if (CollectionUtils.isEmpty(entityIds)) {
            return false;
        }

        boolean result = entityRepository.deleteAllByIdIn(entityIds) > 0;
        if (BooleanUtils.isFalse(result)) {
            return false;
        }

        // Xóa file Training_data.xlsx
//        kafkaTemplate.send(Constant.KafkaTopic.process_after_cud_intent_pattern_entity_entityType, objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
//                .userId(command.getUserId())
//                .build()));
        kafkaConsumer.processAfterCUDIntentPatternEntityEntityType(objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
                .userId(command.getUserId())
                .build()));

        return result;
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase) {
        CommandGetListEntity command = (CommandGetListEntity) commandGetListBase;
        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();

        andCriteriaList.add(Criteria.where("user_id").is(command.getUserId()));

        if (StringUtils.isNotBlank(command.getKeyword())) {
            orCriteriaList.add(Criteria.where("value").regex(command.getKeyword().trim()));
        }

        if (StringUtils.isNotBlank(command.getPatternId())) {
            andCriteriaList.add(Criteria.where("pattern_id").is(command.getPatternId()));
        }

        if (CollectionUtils.isNotEmpty(command.getPatternIds())) {
            andCriteriaList.add(Criteria.where("pattern_id").in(command.getPatternIds()));
        }

        if (CollectionUtils.isNotEmpty(command.getEntityTypeIds())) {
            andCriteriaList.add(Criteria.where("entity_type_id").in(command.getEntityTypeIds()));
        }

        if (CollectionUtils.isNotEmpty(command.getIds())) {
            andCriteriaList.add(Criteria.where("id").in(command.getIds()));
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
        List<EntityEntity> entities = (List<EntityEntity>) entitiesBase;
        CommandGetListEntity command = (CommandGetListEntity) commandGetListBase;

        if (BooleanUtils.isFalse(command.isHasEntityType()) && BooleanUtils.isFalse(command.isHasPattern())) {
            return;
        }

        // Map pattern
        Map<String, PatternEntity> patternsById = new HashMap<>();
        if (BooleanUtils.isTrue(command.isHasPattern())) {
            List<String> patternIds = entities.stream().map(EntityEntity::getPatternId).filter(StringUtils::isNotBlank).toList();
            if (CollectionUtils.isNotEmpty(patternIds)) {
                List<PatternEntity> patterns = patternService.getList(CommandGetListPattern.builder()
                        .ids(patternIds)
                        .userId(command.getUserId())
                        .build(), PatternEntity.class);
                if (CollectionUtils.isNotEmpty(patterns)) {
                    patterns.forEach(p -> patternsById.put(p.getId(), p));
                }
            }
        }

        // Map entity type
        Map<String, EntityTypeEntity> entityTypeById = new HashMap<>();
        if (BooleanUtils.isTrue(command.isHasEntityType())) {
            List<String> entityTypeIds = entities.stream().map(EntityEntity::getEntityTypeId).filter(StringUtils::isNotBlank).toList();
            if (CollectionUtils.isNotEmpty(entityTypeIds)) {
                List<EntityTypeEntity> entityTypes = entityTypeService.getList(CommandGetListEntityType.builder()
                        .userId(command.getUserId())
                        .ids(entityTypeIds)
                        .build(), EntityTypeEntity.class);
                if (CollectionUtils.isNotEmpty(entityTypes)) {
                    entityTypes.forEach(et -> entityTypeById.put(et.getId(), et));
                }
            }
        }

        for (EntityEntity entity : entities) {
            if (BooleanUtils.isTrue(command.isHasEntityType())) {
                entity.entityTypeMapping(entityTypeById);
            }

            if (BooleanUtils.isTrue(command.isHasPattern())) {
                entity.patternMapping(patternsById);
            }
        }
    }
}
