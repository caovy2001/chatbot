package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.entity.IEntityServiceAPI;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/entity")
@Slf4j
public class EntityAPI {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IEntityServiceAPI entityServiceAPI;

    @PostMapping("/")
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    public ResponseEntity<Document> getPaginatedList(@RequestBody CommandGetListEntity command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            command.setUserId(userEntity.getId());
//            Document resMap = objectMapper.convertValue(entityServiceAPI.getPaginatedList(command), Document.class);
            Document resMap = objectMapper.convertValue(entityServiceAPI.getPaginatedList(command, EntityEntity.class, CommandGetListEntity.class), Document.class);
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
