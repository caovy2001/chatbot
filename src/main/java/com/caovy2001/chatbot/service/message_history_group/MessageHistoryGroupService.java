package com.caovy2001.chatbot.service.message_history_group;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.MessageEntityHistoryEntity;
import com.caovy2001.chatbot.entity.MessageHistoryGroupEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.repository.MessageHistoryGroupRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.message_entity_history.IMessageEntityHistoryService;
import com.caovy2001.chatbot.service.message_entity_history.command.CommandGetListMessageEntityHistory;
import com.caovy2001.chatbot.service.message_history_group.command.CommandAddMessageHistoryGroup;
import com.caovy2001.chatbot.service.message_history_group.command.CommandGetListMessageHistoryGroup;
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
public class MessageHistoryGroupService extends BaseService implements IMessageHistoryGroupServiceAPI, IMessageHistoryGroupService {
    @Autowired
    private MessageHistoryGroupRepository messageHistoryGroupRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IMessageEntityHistoryService messageEntityHistoryService;

    @Override
    public MessageHistoryGroupEntity add(@NonNull CommandAddMessageHistoryGroup command) {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getSessionId(), command.getScriptId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        if (command.getCreatedDate() == null) {
            command.setCreatedDate(System.currentTimeMillis());
        }

        MessageHistoryGroupEntity messageHistoryGroup = MessageHistoryGroupEntity.builder()
                .userId(command.getUserId())
                .sessionId(command.getSessionId())
                .scriptId(command.getScriptId())
                .createdDate(command.getCreatedDate())
                .build();
        return messageHistoryGroupRepository.insert(messageHistoryGroup);
    }

    @Override
    public List<MessageHistoryGroupEntity> getList(CommandGetListMessageHistoryGroup command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return null;
        }

        List<MessageHistoryGroupEntity> messageHistoryGroups = mongoTemplate.find(query, MessageHistoryGroupEntity.class);
        this.setViewForMessageHistoryGroupList(messageHistoryGroups, command);
        return messageHistoryGroups;
    }

    @Override
    public Paginated<MessageHistoryGroupEntity> getPaginatedList(CommandGetListMessageHistoryGroup command) throws Exception {
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

        long total = mongoTemplate.count(query, MessageHistoryGroupEntity.class);
        if (total == 0) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        PageRequest pageRequest = PageRequest.of(command.getPage() - 1, command.getSize());
        query.with(pageRequest);
        List<MessageHistoryGroupEntity> messageHistoryGroups = mongoTemplate.find(query, MessageHistoryGroupEntity.class);
        this.setViewForMessageHistoryGroupList(messageHistoryGroups, command);
        return new Paginated<>(messageHistoryGroups, command.getPage(), command.getSize(), total);
    }

    private Query buildQueryGetList(@NonNull CommandGetListMessageHistoryGroup command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();

        andCriteriaList.add(Criteria.where("user_id").is(command.getUserId()));

        if (CollectionUtils.isNotEmpty(command.getIds())) {
            andCriteriaList.add(Criteria.where("id").in(command.getIds()));
        }

        if (StringUtils.isNotBlank(command.getScriptId())) {
            andCriteriaList.add(Criteria.where("script_id").is(command.getScriptId()));
        }

        if (StringUtils.isNotBlank(command.getSessionId())) {
            andCriteriaList.add(Criteria.where("session_id").is(command.getSessionId()));
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

    private void setViewForMessageHistoryGroupList(List<MessageHistoryGroupEntity> messageHistoryGroups, CommandGetListMessageHistoryGroup command) {
        if (CollectionUtils.isEmpty(messageHistoryGroups)) {
            return;
        }

        if (BooleanUtils.isFalse(command.getHasMessageEntityHistories())) {
            return;
        }

        // Map message entity history
        Map<String, List<MessageEntityHistoryEntity>> messageEntityHistoriesBySessionId = new HashMap<>();
        if (BooleanUtils.isTrue(command.getHasMessageEntityHistories())) {
            List<MessageEntityHistoryEntity> messageEntityHistories = messageEntityHistoryService.getList(CommandGetListMessageEntityHistory.builder()
                    .userId(command.getUserId())
                    .sessionIds(messageHistoryGroups.stream().map(MessageHistoryGroupEntity::getSessionId).filter(StringUtils::isNotBlank).toList())
                    .build());

            if (CollectionUtils.isNotEmpty(messageEntityHistories)) {
                for (MessageEntityHistoryEntity messageEntityHistory : messageEntityHistories) {
                    if (messageEntityHistoriesBySessionId.get(messageEntityHistory.getSessionId()) == null) {
                        messageEntityHistoriesBySessionId.put(messageEntityHistory.getSessionId(), List.of(messageEntityHistory));
                    } else {
                        List<MessageEntityHistoryEntity> messageEntityHistoryEntities = messageEntityHistoriesBySessionId.get(messageEntityHistory.getSessionId());
                        List<MessageEntityHistoryEntity> newMessageEntityHistoryEntities = new ArrayList<>(messageEntityHistoryEntities);
                        newMessageEntityHistoryEntities.add(messageEntityHistory);
                        messageEntityHistoriesBySessionId.put(messageEntityHistory.getSessionId(), newMessageEntityHistoryEntities);
                    }
                }
            }
        }

        for (MessageHistoryGroupEntity messageHistoryGroup : messageHistoryGroups) {
            if (BooleanUtils.isTrue(command.getHasMessageEntityHistories())) {
                messageHistoryGroup.setMessageEntityHistories(messageEntityHistoriesBySessionId.get(messageHistoryGroup.getSessionId()));
            }
        }
    }
}
