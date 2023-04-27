package com.caovy2001.chatbot.service.pattern.es;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.entity.es.IntentEntityES;
import com.caovy2001.chatbot.entity.es.PatternEntityES;
import com.caovy2001.chatbot.repository.es.PatternRepositoryES;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.pattern.command.CommandIndexingPatternES;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatternServiceES extends BaseService implements IPatternServiceES {
    @Autowired
    private PatternRepositoryES patternRepositoryES;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void processIndexing(CommandIndexingPatternES command) throws Exception {
        if (StringUtils.isBlank(command.getUserId()) || CollectionUtils.isEmpty(command.getPatterns())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        if (BooleanUtils.isTrue(command.isDoSetUserId())) {
            for (PatternEntity pattern: command.getPatterns()) {
                if (BooleanUtils.isTrue(command.isDoSetUserId())) {
                    pattern.setUserId(command.getUserId());
                }
            }
        }

        List<PatternEntityES> patternESes = objectMapper.readValue(objectMapper.writeValueAsString(command.getPatterns()), new TypeReference<List<PatternEntityES>>() {
        });
        patternRepositoryES.saveAll(patternESes);
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(T commandGetListBase) {
        return null;
    }

    @Override
    protected <Entity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {

    }
}
