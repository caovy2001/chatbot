package com.caovy2001.chatbot.service.entity;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.repository.EntityRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EntityService extends BaseService implements IEntityServiceAPI, IEntityService {
    @Autowired
    private EntityRepository entityRepository;

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
                    commandAddEntity.getStartPosition() >= commandAddEntity.getEndPosition()) {
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
                    entity.getStartPosition() >= entity.getEndPosition()) {
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
}
