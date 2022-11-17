package com.caovy2001.chatbot.service.intent;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.repository.IntentRepository;
import com.caovy2001.chatbot.repository.PatternRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.intent.command.CommandIntent;
import com.caovy2001.chatbot.service.intent.command.CommandIntentAddMany;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAdd;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IntentService extends BaseService implements IIntentService {

    @Autowired
    private IntentRepository intentRepository;

    @Autowired
    private PatternRepository patternRepository;

    @Autowired
    private IPatternService patternService;

    @Override
    public ResponseIntentAdd add(CommandIntent command) {
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
    public ResponseIntentAdd addMany(CommandIntentAddMany command) {
        if (CollectionUtils.isEmpty(command.getIntents()) || StringUtils.isBlank(command.getUserId())) {
            return this.returnException(ExceptionConstant.missing_param, ResponseIntentAdd.class);
        }
        ResponseIntentAdd responseIntentAdd = ResponseIntentAdd.builder()
                .ids(new ArrayList<>())
                .build();
        for(IntentEntity intent: command.getIntents()) {
            IntentEntity existIntent = intentRepository.findByCodeAndUserId(intent.getCode(), command.getUserId()).orElse(null);
            if (existIntent != null) {
                intent.setId(existIntent.getId());
            } else {
                intent.setId(null);
            }

            intent.setUserId(command.getUserId());
            IntentEntity savedIntent = intentRepository.save(intent);
            responseIntentAdd.getIds().add(savedIntent.getId());

            if (!CollectionUtils.isEmpty(intent.getPatterns())) {
                for (PatternEntity pattern: intent.getPatterns()) {
                    CommandPatternAdd commandPatternAdd = CommandPatternAdd.builder()
                            .user_id(command.getUserId())
                            .content(pattern.getContent())
                            .intent_id(savedIntent.getId())
                            .build();
                    patternService.add(commandPatternAdd);
                }
            }
        }

        return responseIntentAdd;
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

    @Override
    public ResponseIntents getById(String id, String userId) {
        //find by user id
        if (id == null){
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
        //find by intent id
        else {
            IntentEntity intent = intentRepository.findById(id).orElse(null);
            List<PatternEntity> patterns = patternRepository.findByIntentIdInAndUserId(
                    intent.getId(),
                    userId);
            intent.setPatterns(patterns);

            return ResponseIntents.builder()
                    .intent(intent)
                    .build();
        }
    }

    @Override
    public ResponseIntents updateName(CommandIntent command, String userId) {
        if (command.getId() == null){
            return  returnException(ExceptionConstant.missing_param, ResponseIntents.class);
        }
        ResponseIntents intent = getById(command.getId(),userId);
        intent.getIntent().setName(command.getName());
        intentRepository.save(intent.getIntent());
        return ResponseIntents.builder().intent(intent.getIntent()).build();
    }

    @Override
    public ResponseIntents deleteIntent(String id, String userId) {
        if (id == null || userId == null){
            return  returnException(ExceptionConstant.missing_param, ResponseIntents.class);
        }
        intentRepository.deleteById(id);
        patternRepository.deleteByIntentIdAndUserId(id, userId);
        return ResponseIntents.builder().build();
    }
}
