package com.caovy2001.chatbot.service.entity_type;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.repository.EntityTypeRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.entity.IEntityService;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;
import com.caovy2001.chatbot.service.entity_type.command.CommandAddEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandEntityTypeAddMany;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandUpdateEntityType;
import com.caovy2001.chatbot.utils.ChatbotStringUtils;
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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EntityTypeService extends BaseService implements IEntityTypeServiceAPI, IEntityTypeService {
    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IEntityService entityService;

    @Override
    public EntityTypeEntity add(CommandAddEntityType command) throws Exception {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getName())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        List<EntityTypeEntity> existEntityTypesByLowerCaseName = this.getList(CommandGetListEntityType.builder()
                .userId(command.getUserId())
                .lowerCaseName(command.getName().trim().toLowerCase())
                .returnFields(List.of("id", "lower_case_name"))
                .build());
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
        EntityTypeEntity resEntityType = entityTypeRepository.insert(entityType);
        if (StringUtils.isBlank(resEntityType.getId())) {
            throw new Exception("add_entity_type_error");
        }

        return resEntityType;
    }

    @Override
    public EntityTypeEntity update(CommandUpdateEntityType command) throws Exception {
        if (StringUtils.isAnyBlank(command.getId(), command.getUserId(), command.getName())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        List<EntityTypeEntity> existEntityTypesById = this.getList(CommandGetListEntityType.builder()
                .id(command.getId())
                .userId(command.getUserId())
                .build());
        if (CollectionUtils.isEmpty(existEntityTypesById)) {
            throw new Exception("entity_type_not_exist");
        }

        List<EntityTypeEntity> existEntityTypesByLowerCaseName = this.getList(CommandGetListEntityType.builder()
                .userId(command.getUserId())
                .lowerCaseName(command.getName().trim().toLowerCase())
                .returnFields(List.of("id", "lower_case_name"))
                .build());
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
            throw new Exception("add_entity_type_error");
        }

        return resEntityType;
    }

    @Override
    public Paginated<EntityTypeEntity> getPaginatedEntityTypeList(@NonNull CommandGetListEntityType command) throws Exception {
        if (StringUtils.isBlank(command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        if (command.getPage() <= 0) {
            throw new Exception("invalid_page_or_size");
        }

        if (command.getSize() <= 0) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        long total = mongoTemplate.count(query, EntityTypeEntity.class);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        PageRequest pageRequest = PageRequest.of(command.getPage() - 1, command.getSize());
        query.with(pageRequest);
        List<EntityTypeEntity> entityTypeEntities = mongoTemplate.find(query, EntityTypeEntity.class);
        this.setViewForListEntityType(entityTypeEntities, command);
        return new Paginated<>(entityTypeEntities, command.getPage(), command.getSize(), total);
    }

    private void setViewForListEntityType(List<EntityTypeEntity> entityTypes, CommandGetListEntityType command) {
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
                    .build());

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

    @Override
    public boolean delete(CommandGetListEntityType command) throws Exception {
        if (StringUtils.isBlank(command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        // Quyết định những trường trả về
        command.setReturnFields(new ArrayList<>());
        command.getReturnFields().add("id");
        command.getReturnFields().add("entities");

        List<EntityTypeEntity> entityTypes = this.getList(command);
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
        }

        return result;
//        return true;
    }

    private Query buildQueryGetList(CommandGetListEntityType command) {
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
    public List<EntityTypeEntity> addMany(@NonNull CommandEntityTypeAddMany command) {
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
                .build());
        Map<String, EntityTypeEntity> existEntityTypeByName = new HashMap<>(); // <entity_type lowercase name, EntityTypeEntity>
        existEntityTypes.forEach(et -> {
            if (StringUtils.isNotBlank(et.getLowerCaseName())) {
                existEntityTypeByName.put(et.getLowerCaseName(), et);
            }
        });
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
        return entityTypeRepository.saveAll(entityTypesToSave);
    }

    @Override
    public List<EntityTypeEntity> getList(CommandGetListEntityType command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return null;
        }

        List<EntityTypeEntity> entityTypes = mongoTemplate.find(query, EntityTypeEntity.class);
        if (CollectionUtils.isEmpty(entityTypes)) {
            return null;
        }

        this.setViewForListEntityType(entityTypes, command);
        return entityTypes;
    }
}
