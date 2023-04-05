package com.caovy2001.chatbot.service.entity;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.repository.EntityRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandGetListPattern;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class EntityService extends BaseService implements IEntityServiceAPI, IEntityService {
    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IEntityTypeService entityTypeService;

    @Autowired
    private IPatternService patternService;
    @Override
    @Deprecated
    public List<EntityEntity> add(List<CommandAddEntity> commandAddEntities) {
        if (CollectionUtils.isEmpty(commandAddEntities)) {
            return null;
        }

        List<EntityEntity> entitiesToAdd = new ArrayList<>();
        for (CommandAddEntity commandAddEntity : commandAddEntities) {
            if (StringUtils.isAnyBlank(commandAddEntity.getUserId(), commandAddEntity.getPatternId(), commandAddEntity.getEntityTypeId(), commandAddEntity.getValue())) {
                log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
                continue;
            }

            if (commandAddEntity.getStartPosition() < 0 ||
                    commandAddEntity.getEndPosition() < 0 ||
                    commandAddEntity.getStartPosition() > commandAddEntity.getEndPosition()) {
                log.error("[{}]: {}", new Exception().getStackTrace()[0], "invalid_start_and_end_position");
                continue;
            }

            entitiesToAdd.add(EntityEntity.builder()
                    .userId(commandAddEntity.getUserId())
                    .patternId(commandAddEntity.getPatternId())
                    .entityTypeId(commandAddEntity.getEntityTypeId())
                    .value(commandAddEntity.getValue())
                    .startPosition(commandAddEntity.getStartPosition())
                    .endPosition(commandAddEntity.getEndPosition())
                    .build());
        }

        if (CollectionUtils.isEmpty(entitiesToAdd)) {
            return null;
        }

        return entityRepository.insert(entitiesToAdd);
    }

    @Override
    public List<EntityEntity> findByUserIdAndPatternId(String userId, String patternId) {
        return entityRepository.findByUserIdAndPatternId(userId, patternId);
    }

    @Override
    public List<EntityEntity> addMany(@NonNull CommandEntityAddMany command) {
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
                    .userId(entity.getUserId())
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

        return entityRepository.insert(entitiesToAdd);
    }

    @Override
    public List<EntityEntity> getList(CommandGetListEntity command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return null;
        }

        List<EntityEntity> entities = mongoTemplate.find(query, EntityEntity.class);
        if (CollectionUtils.isEmpty(entities)) {
            return null;
        }

        this.setViewForListEntities(entities, command);
        return entities;
    }

    @Override
    public boolean delete(CommandGetListEntity command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return false;
        }

        // Quyết định những trường trả về
        command.setReturnFields(List.of("id"));

        List<EntityEntity> entities = this.getList(command);
        if (CollectionUtils.isEmpty(entities)) {
            return false;
        }

        List<String> entityIds = entities.stream().map(EntityEntity::getId).toList();
        if (CollectionUtils.isEmpty(entityIds)) {
            return false;
        }

        return entityRepository.deleteAllByIdIn(entityIds) > 0;
    }

    private Query buildQueryGetList(CommandGetListEntity command) {
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

    private void setViewForListEntities(List<EntityEntity> entities, CommandGetListEntity command) {
        if (BooleanUtils.isFalse(command.isHasEntityType()) && BooleanUtils.isFalse(command.isHasPattern())) {
            return;
        }

        for (EntityEntity entity : entities) {
            if (BooleanUtils.isTrue(command.isHasEntityType())) {
                List<EntityTypeEntity> entityTypes = entityTypeService.getList(CommandGetListEntityType.builder()
                        .userId(command.getUserId())
                        .ids(List.of(entity.getEntityTypeId()))
                        .build());
                if (CollectionUtils.isNotEmpty(entityTypes)) {
                    entity.setEntityType(entityTypes.get(0));
                }
            }

            if (BooleanUtils.isTrue(command.isHasPattern())) {
                List<PatternEntity> patterns = patternService.getList(CommandGetListPattern.builder()
                        .id(entity.getPatternId())
                        .userId(command.getUserId())
                        .build());
                if (CollectionUtils.isNotEmpty(patterns)) {
                    entity.setPattern(patterns.get(0));
                }
            }
        }
    }
}
