package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.node.command.CommandNodeDelete;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAdd;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternDelete;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pattern")
public class PatternAPI {
    @Autowired
    private BaseService baseService;

    @Autowired
    private IPatternService patternService;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/add")
    public ResponseEntity<ResponsePatternAdd> add(@RequestBody CommandPatternAdd command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            ResponsePatternAdd responsePatternAdd = patternService.add(command);
            return ResponseEntity.ok(responsePatternAdd);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePatternAdd.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_all/by_intent_id/{intentId}")
    public ResponseEntity<ResponsePattern> getByIntentId(@PathVariable String intentId) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponsePattern patterns = patternService.getByIntentId(intentId, userEntity.getId());
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_all/by_user_id")
    public ResponseEntity<ResponsePattern> getByUserId() {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponsePattern patterns = patternService.getByUserId(userEntity.getId());
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get/{id}")
    public ResponseEntity<ResponsePattern> getById(@PathVariable String id) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponsePattern patterns = patternService.getById(id, userEntity.getId());
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @DeleteMapping()
    public ResponseEntity<ResponsePatternAdd> delete(@RequestBody CommandPatternDelete command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponsePatternAdd responsePatternAdd = patternService.delete(command.getId());
            return ResponseEntity.ok(responsePatternAdd);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePatternAdd.class));
        }
    }
}
