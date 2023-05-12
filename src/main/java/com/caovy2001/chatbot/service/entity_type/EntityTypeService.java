package com.caovy2001.chatbot.service.entity_type;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.repository.EntityTypeRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.entity.IEntityService;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;
import com.caovy2001.chatbot.service.entity_type.command.CommandAddEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandEntityTypeAddMany;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandUpdateEntityType;
import com.caovy2001.chatbot.service.pattern.command.CommandProcessAfterCUDIntentPatternEntityEntityType;
import com.caovy2001.chatbot.utils.ChatbotStringUtils;
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
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EntityTypeService extends BaseService implements IEntityTypeService {
    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IEntityService entityService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public <Entity extends BaseEntity, CommandAddMany extends CommandAddManyBase> List<Entity> add(CommandAddMany commandAddManyBase) throws Exception {
        CommandEntityTypeAddMany command = (CommandEntityTypeAddMany) commandAddManyBase;

        if (StringUtils.isBlank(command.getUserId()) || CollectionUtils.isEmpty(command.getEntityTypes())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        //region Tìm những entity đã tồn tại theo name được truyền lên
        List<String> lowerCaseNames = new ArrayList<>();
        command.getEntityTypes().forEach(et -> {
            if (StringUtils.isNotBlank(et.getName())) {
                lowerCaseNames.add(et.getName().trim().toLowerCase());
            }
        });
        List<EntityTypeEntity> existEntityTypes = this.getList(CommandGetListEntityType.builder()
                .userId(command.getUserId())
                .lowerCaseNames(lowerCaseNames)
                .build(), EntityTypeEntity.class);
        Map<String, EntityTypeEntity> existEntityTypeByName = new HashMap<>(); // <entity_type lowercase name, EntityTypeEntity>
        if (CollectionUtils.isNotEmpty(existEntityTypes)) {
            existEntityTypes.forEach(et -> {
                if (StringUtils.isNotBlank(et.getLowerCaseName())) {
                    existEntityTypeByName.put(et.getLowerCaseName(), et);
                }
            });
        }
        //endregion

        List<EntityTypeEntity> entityTypesToSave = new ArrayList<>();
        for (EntityTypeEntity entityType : command.getEntityTypes()) {
            if (StringUtils.isBlank(entityType.getName())) {
                log.error("[{}|{}]: {}", command.getUserId(), new Exception().getStackTrace()[0], "name_null");
                continue;
            }

            EntityTypeEntity entityTypeToSave = EntityTypeEntity.builder()
                    .userId(command.getUserId())
                    .name(entityType.getName())
                    .uuid(StringUtils.isNotBlank(entityType.getUuid()) ? entityType.getUuid() : UUID.randomUUID().toString())
                    .lowerCaseName(entityType.getName().trim().toLowerCase())
                    .searchableName(ChatbotStringUtils.stripAccents(entityType.getName().trim().toLowerCase()))
                    .build();
            // Entity type đã tồn tại thì cập nhật
            EntityTypeEntity existEntityType = existEntityTypeByName.get(entityTypeToSave.getLowerCaseName());
            if (existEntityType != null) {
                entityTypeToSave.setId(existEntityType.getId());
                entityTypeToSave.setCreatedDate(existEntityType.getCreatedDate());
            }
            entityTypesToSave.add(entityTypeToSave);
        }

        List<EntityTypeEntity> entityTypes = entityTypeRepository.saveAll(entityTypesToSave);
        if (CollectionUtils.isEmpty(entityTypes)) {
            throw new Exception(ExceptionConstant.error_occur);
        }

        // Xóa file Training_data.xlsx
        kafkaTemplate.send(Constant.KafkaTopic.process_after_cud_intent_pattern_entity_entityType, objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
                .userId(command.getUserId())
                .build()));

        return (List<Entity>) entityTypes;
    }

    @Override
    public <Entity extends BaseEntity, CommandAdd extends CommandAddBase> Entity add(CommandAdd commandAddBase) throws Exception {
        CommandAddEntityType command = (CommandAddEntityType) commandAddBase;

        if (StringUtils.isAnyBlank(command.getUserId(), command.getName())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        List<EntityTypeEntity> existEntityTypesByLowerCaseName = this.getList(CommandGetListEntityType.builder()
                .userId(command.getUserId())
                .lowerCaseName(command.getName().trim().toLowerCase())
                .returnFields(List.of("id", "lower_case_name"))
                .build(), EntityTypeEntity.class);
        if (CollectionUtils.isNotEmpty(existEntityTypesByLowerCaseName)) {
            throw new Exception("name_exist");
        }

        EntityTypeEntity entityType = EntityTypeEntity.builder()
                .userId(command.getUserId())
                .name(command.getName().trim())
                .uuid(StringUtils.isNotBlank(command.getUuid()) ? command.getUuid() : UUID.randomUUID().toString())
                .lowerCaseName(command.getName().trim().toLowerCase())
                .searchableName(ChatbotStringUtils.stripAccents(command.getName().trim().toLowerCase()))
                .build();
        List<EntityTypeEntity> resEntityTypes = this.add(CommandEntityTypeAddMany.builder()
                .userId(command.getUserId())
                .entityTypes(List.of(entityType))
                .build());
        if (CollectionUtils.isEmpty(resEntityTypes)) {
            throw new Exception("add_entity_type_error");
        }

        return (Entity) resEntityTypes.get(0);
    }

    @Override
    public <Entity extends BaseEntity, CommandUpdate extends CommandUpdateBase> Entity update(CommandUpdate commandUpdateBase) throws Exception {
        CommandUpdateEntityType command = (CommandUpdateEntityType) commandUpdateBase;

        if (StringUtils.isAnyBlank(command.getId(), command.getUserId(), command.getName())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        List<EntityTypeEntity> existEntityTypesById = this.getList(CommandGetListEntityType.builder()
                .id(command.getId())
                .userId(command.getUserId())
                .build(), EntityTypeEntity.class);
        if (CollectionUtils.isEmpty(existEntityTypesById)) {
            throw new Exception("entity_type_not_exist");
        }

        List<EntityTypeEntity> existEntityTypesByLowerCaseName = this.getList(CommandGetListEntityType.builder()
                .userId(command.getUserId())
                .lowerCaseName(command.getName().trim().toLowerCase())
                .returnFields(List.of("id", "lower_case_name"))
                .build(), EntityTypeEntity.class);
        if (CollectionUtils.isNotEmpty(existEntityTypesByLowerCaseName)) {
            throw new Exception("name_exist");
        }

        EntityTypeEntity entityType = existEntityTypesById.get(0);
        entityType.setName(command.getName().trim());
        entityType.setLowerCaseName(command.getName().trim().toLowerCase());
        entityType.setSearchableName(ChatbotStringUtils.stripAccents(command.getName().trim().toLowerCase()));
        entityType.setLastUpdatedDate(System.currentTimeMillis());
        EntityTypeEntity resEntityType = entityTypeRepository.save(entityType);
        if (StringUtils.isBlank(resEntityType.getId())) {
            throw new Exception("update_entity_type_error");
        }

        // Xóa file Training_data.xlsx
        kafkaTemplate.send(Constant.KafkaTopic.process_after_cud_intent_pattern_entity_entityType, objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
                .userId(command.getUserId())
                .build()));

        return (Entity) resEntityType;
    }

    @Override
    public <CommandGetList extends CommandGetListBase> boolean delete(CommandGetList commandGetListBase) throws Exception {
        CommandGetListEntityType command = (CommandGetListEntityType) commandGetListBase;

        if (StringUtils.isBlank(command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        // Quyết định những trường trả về
        command.setReturnFields(new ArrayList<>());
        command.getReturnFields().add("id");
        command.getReturnFields().add("entities");

        List<EntityTypeEntity> entityTypes = this.getList(command, EntityTypeEntity.class);
        if (CollectionUtils.isEmpty(entityTypes)) {
            return false;
        }

        List<String> entityTypeIds = entityTypes.stream().map(EntityTypeEntity::getId).toList();
        if (CollectionUtils.isEmpty(entityTypeIds)) {
            return false;
        }
        boolean result = entityTypeRepository.deleteAllByIdIn(entityTypeIds) > 0;

        if (BooleanUtils.isTrue(result)) {
            if (BooleanUtils.isTrue(command.isHasEntities())) {
                CompletableFuture.runAsync(() -> {
                    try {
                        entityService.delete(CommandGetListEntity.builder()
                                .userId(command.getUserId())
                                .entityTypeIds(entityTypeIds)
                                .build());
                    } catch (Exception e) {
                        log.info(e.getMessage());
                    }
                });
            }

            // Xóa file Training_data.xlsx
            kafkaTemplate.send(Constant.KafkaTopic.process_after_cud_intent_pattern_entity_entityType, objectMapper.writeValueAsString(CommandProcessAfterCUDIntentPatternEntityEntityType.builder()
                    .userId(command.getUserId())
                    .build()));
        }

        return result;
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase) {
        CommandGetListEntityType command = (CommandGetListEntityType) commandGetListBase;

        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();

        andCriteriaList.add(Criteria.where("user_id").is(command.getUserId()));

        if (StringUtils.isNotBlank(command.getKeyword())) {
            orCriteriaList.add(Criteria.where("searchable_name").regex(ChatbotStringUtils.stripAccents(command.getKeyword().trim().toLowerCase())));
        }

        if (StringUtils.isNotBlank(command.getId())) {
            andCriteriaList.add(Criteria.where("id").is(command.getId()));
        }

        if (CollectionUtils.isNotEmpty(command.getIds())) {
            andCriteriaList.add(Criteria.where("id").in(command.getIds()));
        }

        if (StringUtils.isNotBlank(command.getLowerCaseName())) {
            andCriteriaList.add(Criteria.where("lower_case_name").is(command.getLowerCaseName()));
        }

        if (CollectionUtils.isNotEmpty(command.getLowerCaseNames())) {
            andCriteriaList.add(Criteria.where("lower_case_name").in(command.getLowerCaseNames()));
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
            List<String> returnFields = new ArrayList<>(command.getReturnFields());
            returnFields.removeAll(Collections.singletonList("entities"));
            query.fields().include(Arrays.copyOf(returnFields.toArray(), returnFields.size(), String[].class));
        }
        return query;
    }

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {
        List<EntityTypeEntity> entityTypes = (List<EntityTypeEntity>) entitiesBase;
        CommandGetListEntityType command = (CommandGetListEntityType) commandGetListBase;

        if (CollectionUtils.isEmpty(entityTypes)) {
            return;
        }

        if ((BooleanUtils.isFalse(command.isHasEntities()) || (CollectionUtils.isNotEmpty(command.getReturnFields()) && !command.getReturnFields().contains("entities")))) {
            return;
        }

        // Lấy toàn bộ entity của toàn bộ entity type
        Map<String, List<EntityEntity>> entitiesByEntityTypeId = new HashMap<>();
        if (BooleanUtils.isTrue(command.isHasEntities()) &&
                (CollectionUtils.isEmpty(command.getReturnFields()) || command.getReturnFields().contains("entities"))) {
            List<EntityEntity> entities = entityService.getList(CommandGetListEntity.builder()
                    .userId(command.getUserId())
                    .entityTypeIds(entityTypes.stream().map(EntityTypeEntity::getId).toList())
                    .hasPattern(command.isHasPatternOfEntities())
                    .build(), EntityEntity.class);

            if (CollectionUtils.isNotEmpty(entities)) {
                for (EntityEntity entity : entities) {
                    List<EntityEntity> entitiesByEntityTypeIdTmp = entitiesByEntityTypeId.get(entity.getEntityTypeId());
                    if (CollectionUtils.isEmpty(entitiesByEntityTypeIdTmp)) {
                        entitiesByEntityTypeIdTmp = new ArrayList<>();
                    }
                    entitiesByEntityTypeIdTmp.add(entity);
                    entitiesByEntityTypeId.put(entity.getEntityTypeId(), entitiesByEntityTypeIdTmp);
                }
            }
        }

        for (EntityTypeEntity entityType : entityTypes) {
            if (BooleanUtils.isTrue(command.isHasEntities()) &&
                    (CollectionUtils.isEmpty(command.getReturnFields()) || command.getReturnFields().contains("entities"))) {
                entityType.setEntities(entitiesByEntityTypeId.get(entityType.getId()));
            }
        }
    }
}
