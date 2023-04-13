package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeServiceAPI;
import com.caovy2001.chatbot.service.entity_type.command.CommandAddEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandGetListEntityType;
import com.caovy2001.chatbot.service.entity_type.command.CommandUpdateEntityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/entity_type")
@Slf4j
public class EntityTypeAPI {
    @Autowired
    private IEntityTypeServiceAPI entityTypeServiceAPI;

    @Autowired
    private IBaseService baseService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    public ResponseEntity<Document> add(@RequestBody CommandAddEntityType command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            command.setUserId(userEntity.getId());
            Document resMap = objectMapper.convertValue(entityTypeServiceAPI.add(command), Document.class);
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

    @PostMapping("/update")
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    public ResponseEntity<Document> update(@RequestBody CommandUpdateEntityType command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            command.setUserId(userEntity.getId());
            Document resMap = objectMapper.convertValue(entityTypeServiceAPI.update(command), Document.class);
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

    @PostMapping("/")
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    public ResponseEntity<Document> getPaginatedList(@RequestBody CommandGetListEntityType command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            command.setUserId(userEntity.getId());
            Document resMap = objectMapper.convertValue(entityTypeServiceAPI.getPaginatedEntityTypeList(command), Document.class);
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

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    public ResponseEntity<Document> getDetail(@PathVariable String id) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            CommandGetListEntityType command = CommandGetListEntityType.builder()
                    .userId(userEntity.getId())
                    .id(id)
                    .page(1)
                    .size(1)
                    .build();

            List<EntityTypeEntity> entityTypes = entityTypeServiceAPI.getPaginatedEntityTypeList(command).getItems();
            if (CollectionUtils.isEmpty(entityTypes)) {
                throw new Exception("entity_type_not_exist");
            }

            Document resMap = objectMapper.convertValue(entityTypes.get(0), Document.class);
            resMap.put("http_status", "OK");
            return ResponseEntity.ok(resMap);
        } catch (Exception e) {
            Document resMap = new Document();
            resMap.put("http_status", "EXPECTATION_FAILED");
            resMap.put("exception_code", StringUtils.isNotBlank(e.getMessage())? e.getMessage() : ExceptionConstant.error_occur);
            return ResponseEntity.ok(resMap);
        }
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    public ResponseEntity<Document> delete(@RequestBody CommandGetListEntityType command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            command.setUserId(userEntity.getId());
            command.setHasEntities(true);

            if (BooleanUtils.isFalse(entityTypeServiceAPI.delete(command))) {
                throw new Exception("delete_fail");
            }

            Document resMap = new Document();
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
