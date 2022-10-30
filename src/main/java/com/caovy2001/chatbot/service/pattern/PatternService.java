package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.repository.PatternRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAdd;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatternService extends BaseService implements IPatternService {
    @Autowired
    private PatternRepository patternRepository;

    @Override
    public ResponsePatternAdd add(CommandPatternAdd command) {
        if (StringUtils.isAnyBlank(command.getUser_id(), command.getContent(), command.getIntent_id())) {
            return returnException(ExceptionConstant.missing_param, ResponsePatternAdd.class);
        }

        PatternEntity pattern = PatternEntity.builder()
                .content(command.getContent())
                .intentId(command.getIntent_id())
                .userId(command.getUser_id())
                .build();
        PatternEntity addedPattern = patternRepository.insert(pattern);
        return ResponsePatternAdd.builder()
                .id(addedPattern.getId())
                .build();
    }
}
