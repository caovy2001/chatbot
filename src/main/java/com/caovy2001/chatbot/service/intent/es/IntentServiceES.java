package com.caovy2001.chatbot.service.intent.es;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.es.IntentEntityES;
import com.caovy2001.chatbot.repository.es.IntentRepositoryES;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.intent.command.CommandDeleteIntentES;
import com.caovy2001.chatbot.service.intent.command.CommandIndexingIntentES;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntentServiceES extends BaseService implements IIntentServiceES {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IntentRepositoryES intentRepositoryES;

    @Override
    public void index(CommandIndexingIntentES command) throws Exception {
        if (StringUtils.isBlank(command.getUserId()) || CollectionUtils.isEmpty(command.getIntents())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        if (BooleanUtils.isTrue(command.isDoSetUserId())) {
            for (IntentEntity intent: command.getIntents()) {
                if (BooleanUtils.isTrue(command.isDoSetUserId())) {
                    intent.setUserId(command.getUserId());
                }
            }
        }

        List<IntentEntityES> intentEntityESes = objectMapper.readValue(objectMapper.writeValueAsString(command.getIntents()), new TypeReference<List<IntentEntityES>>() {
        });

        intentRepositoryES.saveAll(intentEntityESes);
    }

    @Override
    public void delete(CommandDeleteIntentES command) throws Exception {
        if (CollectionUtils.isEmpty(command.getIds())) {
            return;
        }

        intentRepositoryES.deleteAllById(command.getIds());
    }


}
