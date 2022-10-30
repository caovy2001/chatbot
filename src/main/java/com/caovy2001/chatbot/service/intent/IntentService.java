package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.repository.IntentRepository;
import com.caovy2001.chatbot.repository.PatternRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.intent.command.CommandIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IntentService extends BaseService implements IIntentService {

    @Autowired
    private IntentRepository intentRepository;

    @Autowired
    private PatternRepository patternRepository;

    @Override
    public ResponseIntentAdd add(CommandIntentAdd command) {
        if (StringUtils.isAnyBlank(command.getCode(), command.getName(), command.getUser_id())) {
            return returnException(ExceptionConstant.missing_param, ResponseIntentAdd.class);
        }

        IntentEntity existIntent = intentRepository.findByCodeAndUserId(command.getCode(), command.getUser_id()).orElse(null);
        if (existIntent != null) {
            return returnException("intent_code_exist", ResponseIntentAdd.class);
        }

        IntentEntity intent = IntentEntity.builder()
                .code(command.getCode())
                .name(command.getName())
                .userId(command.getUser_id())
                .build();

        IntentEntity addedIntent = intentRepository.insert(intent);
        return ResponseIntentAdd.builder()
                .id(addedIntent.getId())
                .build();
    }

    @Override
    public ResponseIntents getByUserId(String userId) {
        if (StringUtils.isBlank(userId))
            return returnException(ExceptionConstant.missing_param, ResponseIntents.class);

        List<IntentEntity> intents = intentRepository.findByUserId(userId);
        List<PatternEntity> patterns = patternRepository.findByIntentIdInAndUserId(
                intents.stream().map(IntentEntity::getId).collect(Collectors.toList()),
                userId);

        for (IntentEntity intent: intents) {
            List<PatternEntity> patternsByIntent = patterns.stream()
                    .filter(patternEntity -> patternEntity.getIntentId().equals(intent.getId())).collect(Collectors.toList());
            intent.setPatterns(patternsByIntent);
        }

        return ResponseIntents.builder()
                .intents(intents)
                .build();
    }
}
