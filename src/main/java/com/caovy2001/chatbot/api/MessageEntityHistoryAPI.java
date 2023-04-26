package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.message_entity_history.IMessageEntityHistoryServiceAPI;
import com.caovy2001.chatbot.service.message_entity_history.command.CommandGetListMessageEntityHistory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/message_entity_history")
public class MessageEntityHistoryAPI {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IMessageEntityHistoryServiceAPI messageEntityHistoryServiceAPI;

    @PostMapping("/")
    public ResponseEntity<Document> getPaginatedList(@RequestBody CommandGetListMessageEntityHistory command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            command.setUserId(userEntity.getId());
            Document resMap = objectMapper.convertValue(messageEntityHistoryServiceAPI.getPaginatedList(command), Document.class);
            if (resMap == null) {
                throw new Exception("cannot_parse_result");
            }
            resMap.put("http_status", "OK");
            return ResponseEntity.ok(resMap);
        } catch (Exception e) {
            Document resMap = new Document();
            resMap.put("http_status", "EXPECTATION_FAILED");
            resMap.put("exception_code", StringUtils.isNotBlank(e.getMessage())? e.getMessage() : ExceptionConstant.error_occur);
            return ResponseEntity.ok(resMap);
        }
    }
}
