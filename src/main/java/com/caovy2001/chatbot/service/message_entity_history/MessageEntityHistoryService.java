package com.caovy2001.chatbot.service.message_entity_history;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.entity.MessageEntityHistoryEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.repository.MessageEntityHistoryRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.message_entity_history.command.CommandAddMessageEntityHistory;
import com.caovy2001.chatbot.service.message_entity_history.command.CommandAppendValuesMessageEntityHistory;
import com.caovy2001.chatbot.service.message_entity_history.command.CommandGetListMessageEntityHistory;
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

@Service
@Slf4j
public class MessageEntityHistoryService extends BaseService implements IMessageEntityHistoryServiceAPI, IMessageEntityHistoryService {

    @Autowired
    private MessageEntityHistoryRepository messageEntityHistoryRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IEntityTypeService entityTypeService;

    @Override
    public MessageEntityHistoryEntity add(CommandAddMessageEntityHistory command) {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getSessionId(), command.getEntityTypeId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        if (CollectionUtils.isEmpty(command.getValues())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], "message_entity_history_must_have_at_least_1_value");
            return null;
        }

        // Check exist
        if (BooleanUtils.isTrue(command.isCheckExist())) {
            List<MessageEntityHistoryEntity> messageEntityHistories = this.getList(CommandGetListMessageEntityHistory.builder()
                    .userId(command.getUserId())
                    .sessionId(command.getSessionId())
                    .entityTypeId(command.getEntityTypeId())
                    .returnFields(List.of("id"))
                    .build());
            if (CollectionUtils.isNotEmpty(messageEntityHistories)) {
                log.error("[{}]: {}", new Exception().getStackTrace()[0], "message_entity_history_exist");
                return null;
            }
        }

        MessageEntityHistoryEntity messageEntityHistory = MessageEntityHistoryEntity.builder()
                .userId(command.getUserId())
                .entityTypeId(command.getEntityTypeId())
                .sessionId(command.getSessionId())
                .values(command.getValues())
                .build();
        return messageEntityHistoryRepository.insert(messageEntityHistory);
    }

    @Override
    public List<MessageEntityHistoryEntity> getList(CommandGetListMessageEntityHistory command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return null;
        }

        List<MessageEntityHistoryEntity> messageEntityHistories = mongoTemplate.find(query, MessageEntityHistoryEntity.class);
        this.setViewForListMessageEntityHistories(messageEntityHistories, command);
        return messageEntityHistories;
    }

    private Query buildQueryGetList(CommandGetListMessageEntityHistory command) {
        if (StringUtils.isBlank(command.getUserId())) {
            return null;
        }

        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();

        andCriteriaList.add(Criteria.where("user_id").is(command.getUserId()));

        if (StringUtils.isNotBlank(command.getSessionId())) {
            andCriteriaList.add(Criteria.where("session_id").is(command.getSessionId()));
        }

        if (CollectionUtils.isNotEmpty(command.getSessionIds())) {
            andCriteriaList.add(Criteria.where("session_id").in(command.getSessionIds()));
        }

        if (StringUtils.isNotBlank(command.getEntityTypeId())) {
            andCriteriaList.add(Criteria.where("entity_type_id").is(command.getEntityTypeId()));
        }

        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList);
        }

        if (CollectionUtils.isNotEmpty(andCriteriaList  )) {
            criteria.andOperator(andCriteriaList);
        }
        query.addCriteria(criteria);
        if (CollectionUtils.isNotEmpty(command.getReturnFields())) {
            List<String> returnFields = new ArrayList<>(command.getReturnFields());
            returnFields.remove("entity_type");
            query.fields().include(Arrays.copyOf(returnFields.toArray(), returnFields.size(), String[].class));
        }
        return query;
    }

    private void setViewForListMessageEntityHistories(List<MessageEntityHistoryEntity> messageEntityHistories, CommandGetListMessageEntityHistory command) {
        if (CollectionUtils.isEmpty(messageEntityHistories)) {
            return;
        }

        if (BooleanUtils.isFalse(command.getHasEntityType())) {
            return;
        }

        // Map entity type
        Map<String, EntityTypeEntity> entityTypeById = new HashMap<>();
        if (BooleanUtils.isTrue(command.getHasEntityType())) {
            List<EntityTypeEntity> entityTypes = entityTypeService.getList(CommandGetListEntityType.builder()
                    .userId(command.getUserId())
                    .ids(messageEntityHistories.stream().map(MessageEntityHistoryEntity::getEntityTypeId).filter(StringUtils::isNotBlank).toList())
                    .build(), EntityTypeEntity.class);
            if (CollectionUtils.isNotEmpty(entityTypes)) {
                for (EntityTypeEntity entityType : entityTypes) {
                    entityTypeById.put(entityType.getId(), entityType);
                }
            }
        }

        for (MessageEntityHistoryEntity messageEntityHistory : messageEntityHistories) {
            if (BooleanUtils.isTrue(command.getHasEntityType())) {
                messageEntityHistory.setEntityType(entityTypeById.get(messageEntityHistory.getEntityTypeId()));
            }
        }
    }

    @Override
    public MessageEntityHistoryEntity checkExistAndAdd(@NonNull CommandGetListMessageEntityHistory commandCheckExist,
                                                       @NonNull CommandAddMessageEntityHistory commandAdd) {
        if (StringUtils.isAnyBlank(commandCheckExist.getUserId(), commandAdd.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        // Check exist
        List<MessageEntityHistoryEntity> messageEntityHistories = this.getList(CommandGetListMessageEntityHistory.builder()
                .userId(commandCheckExist.getUserId())
                .sessionId(commandCheckExist.getSessionId())
                .entityTypeId(commandCheckExist.getEntityTypeId())
                .returnFields(List.of("id"))
                .build());
        if (CollectionUtils.isNotEmpty(messageEntityHistories)) {
            return messageEntityHistories.get(0);
        }

        return this.add(commandAdd);
    }

    @Override
    public MessageEntityHistoryEntity appendValues(CommandAppendValuesMessageEntityHistory command) {
        if (command.getCommandGet() == null ||
                CollectionUtils.isEmpty(command.getValues())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        List<MessageEntityHistoryEntity> messageEntityHistories = this.getList(command.getCommandGet());
        MessageEntityHistoryEntity messageEntityHistory = null;
        if (CollectionUtils.isEmpty(messageEntityHistories)) {
            if (command.isAddWhenGetNull()) {
                return this.add(CommandAddMessageEntityHistory.builder()
                        .userId(command.getCommandGet().getUserId())
                        .sessionId(command.getCommandGet().getSessionId())
                        .entityTypeId(command.getCommandGet().getEntityTypeId())
                        .values(command.getValues())
                        .checkExist(false)
                        .build());
            } else {
                log.error("[{}]: {}", new Exception().getStackTrace()[0], "message_entity_history_null");
                return null;
            }
        } else {
            messageEntityHistory = messageEntityHistories.get(0);
        }

        messageEntityHistory.getValues().addAll(command.getValues());
        return messageEntityHistoryRepository.save(messageEntityHistory);
    }

    @Override
    public Paginated<MessageEntityHistoryEntity> getPaginatedList(CommandGetListMessageEntityHistory command) throws Exception {
        if (StringUtils.isBlank(command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        if (command.getPage() <= 0 || command.getSize() <= 0) {
            throw new Exception("invalid_page_or_size");
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        long total = mongoTemplate.count(query, MessageEntityHistoryEntity.class);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        PageRequest pageRequest = PageRequest.of(command.getPage() - 1, command.getSize());
        query.with(pageRequest);
        List<MessageEntityHistoryEntity> messageEntityHistories = mongoTemplate.find(query, MessageEntityHistoryEntity.class);
        this.setViewForListMessageEntityHistories(messageEntityHistories, command);
        return new Paginated<>(messageEntityHistories, command.getPage(), command.getSize(), total);
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase) {
        return null;
    }

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {

    }
}
