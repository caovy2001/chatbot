package com.caovy2001.chatbot.service.kafka;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.service.intent.command.CommandIndexingIntentES;
import com.caovy2001.chatbot.service.intent.es.IIntentServiceES;
import com.caovy2001.chatbot.service.message_history.IMessageHistoryService;
import com.caovy2001.chatbot.service.message_history.command.CommandAddMessageHistory;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandIndexingPatternES;
import com.caovy2001.chatbot.service.pattern.command.CommandProcessAfterCUDIntentPatternEntityEntityType;
import com.caovy2001.chatbot.service.pattern.es.IPatternServiceES;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class KafkaConsumer {
    @Autowired
    private IMessageHistoryService messageHistoryService;

    @Autowired
    private IIntentServiceES intentServiceES;

    @Autowired
    private IPatternServiceES patternServiceES;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IPatternService patternService;

//    @KafkaListener(topics = Constant.KafkaTopic.process_save_message_when_predict, groupId = "group_id")
    public void processSaveMessageWhenPredictConsumer(String message) throws IOException {
        try {
            log.info("[{}]: {}", "Consumer process_save_message_when_predict", message);
            messageHistoryService.add(objectMapper.readValue(message, CommandAddMessageHistory.class));
        } catch (Exception e) {
            log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
        }
    }

//    @KafkaListener(topics = Constant.KafkaTopic.process_indexing_intent_es, groupId = "group_id")
    public void processIndexingIntentES(String message) throws IOException {
        try {
            log.info("[{}]: {}", "Consumer process_indexing_intent_es ", message);
            intentServiceES.index(objectMapper.readValue(message, CommandIndexingIntentES.class));
        } catch (Exception e) {
            log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage())? e.getMessage(): ExceptionConstant.error_occur);
        }
    }

//    @KafkaListener(topics = Constant.KafkaTopic.process_indexing_pattern_es, groupId = "group_id")
    public void processIndexingPatternES(String message) throws IOException {
        try {
            log.info("[{}]: {}", "Consumer process_indexing_pattern_es", message);
            patternServiceES.processIndexing(objectMapper.readValue(message, CommandIndexingPatternES.class));
        } catch (Exception e) {
            log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage())? e.getMessage(): ExceptionConstant.error_occur);
        }
    }

//    @KafkaListener(topics = Constant.KafkaTopic.process_after_cud_intent_pattern_entity_entityType, groupId = "group_id")
    public void processAfterCUDIntentPatternEntityEntityType(String message) throws Exception {
        try {
            log.info("[{}]: {}", "Consumer process_after_cud_intent_pattern_entity_entityType", message);
            patternService.processAfterCUDIntentPatternEntityEntityType(objectMapper.readValue(message, CommandProcessAfterCUDIntentPatternEntityEntityType.class));
        } catch (Exception e) {
            log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage())? e.getMessage(): ExceptionConstant.error_occur);
        }
    }
}
