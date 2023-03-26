package com.caovy2001.chatbot.service.script_intent_mapping;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.ScriptIntentMappingEntity;
import com.caovy2001.chatbot.repository.ScriptIntentMappingRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.script_intent_mapping.command.CommandGetListScriptIntentMapping;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
public class ScriptIntentMappingService extends BaseService implements IScriptIntentMappingService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ScriptIntentMappingRepository scriptIntentMappingRepository;

    @Override
    public List<ScriptIntentMappingEntity> getList(CommandGetListScriptIntentMapping command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return null;
        }

        return mongoTemplate.find(query, ScriptIntentMappingEntity.class);
    }

    @Override
    public boolean addForScriptIdByIntentIds(@NonNull String userId, @NonNull String scriptId, List<String> intentIds) {
        try {
            if (CollectionUtils.isEmpty(intentIds)) {
                log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
                return false;
            }

            List<ScriptIntentMappingEntity> scriptIntentMappingEntities = new ArrayList<>();
            for (String intentId: intentIds) {
                ScriptIntentMappingEntity scriptIntentMappingEntity = ScriptIntentMappingEntity.builder()
                        .userId(userId)
                        .intentId(intentId)
                        .scriptId(scriptId)
                        .build();
                scriptIntentMappingEntities.add(scriptIntentMappingEntity);
            }

            scriptIntentMappingRepository.insert(scriptIntentMappingEntities);
            return true;
        } catch (Exception e) {
            log.error("[{}]: {}", e.getStackTrace()[0], e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteByScriptId(@NonNull String userId, @NonNull String scriptId) {
        try {
            scriptIntentMappingRepository.deleteByUserIdAndScriptId(userId, scriptId);
            return true;
        } catch (Exception e) {
            log.error("[{}]: {}", e.getStackTrace()[0], e.getMessage());
            return false;
        }
    }

    private Query buildQueryGetList(CommandGetListScriptIntentMapping command) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();

        andCriteriaList.add(Criteria.where("user_id").is(command.getUserId()));

        if (CollectionUtils.isNotEmpty(command.getScriptIds())) {
            andCriteriaList.add(Criteria.where("script_id").in(command.getScriptIds()));
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
}
