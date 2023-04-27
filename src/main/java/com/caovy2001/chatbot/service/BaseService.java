package com.caovy2001.chatbot.service;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public abstract class BaseService implements IBaseService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public <T extends ResponseBase> T returnException(String exceptionCode, Class<T> clazz) {
        try {
            T entity = clazz.getDeclaredConstructor().newInstance();
            entity.setHttpStatus(HttpStatus.EXPECTATION_FAILED);
            entity.setExceptionCode(exceptionCode);
            return entity;
        } catch (Exception e) {
            log.error(e.toString());
            return null;
        }
    }

    @Override
    public <E extends BaseEntity, Command extends CommandGetListBase> Paginated<E> getPaginatedList(Command command, Class<E> entityClass, Class<Command> commandClass) throws Exception {
        if (StringUtils.isBlank(command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        if (command.getPage() <= 0 || command.getSize() < 0) {
            throw new Exception("invalid_page_or_size");
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        long total = mongoTemplate.count(query, entityClass);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        Command commandGetListNew = objectMapper.readValue(objectMapper.writeValueAsString(command), commandClass);
        commandGetListNew.setCheckPageAndSize(true);
        commandGetListNew.setQuery(query);
        List<E> entities = this.getList(commandGetListNew, entityClass);
        if (CollectionUtils.isEmpty(entities)) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), total);
        }

        return new Paginated<>(entities, command.getPage(), command.getSize(), total);
    }

    @Override
    public <T> List<T> getList(CommandGetListBase command, Class<T> entityType) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = null;
        if (command.getQuery() != null) {
            query = command.getQuery();
        } else {
            query = this.buildQueryGetList(command);
            if (query == null) {
                return null;
            }
        }

        if (BooleanUtils.isTrue(command.isCheckPageAndSize())) {
            if (command.getPage() <= 0 || command.getSize() < 0) {
                log.error("[{}]: {}", new Exception().getStackTrace()[0], "invalid_page_or_size");
                return null;
            }

            PageRequest pageRequest = PageRequest.of(command.getPage() - 1, command.getSize());
            query.with(pageRequest);
        }

        List<T> entities = mongoTemplate.find(query, entityType);
        if (CollectionUtils.isEmpty(entities)) {
            return null;
        }

        this.setViews(entities, command);
        return entities;
    }

    protected abstract <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase);

    protected abstract <Entity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase);
}
