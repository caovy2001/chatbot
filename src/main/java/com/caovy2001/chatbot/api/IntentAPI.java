package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.command.CommandIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/intent")
public class IntentAPI {
    @Autowired
    private IBaseService baseService;

    @Autowired
    private IIntentService intentService;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/add")
    public ResponseEntity<ResponseIntentAdd> add(@RequestBody CommandIntentAdd command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUser_id(userEntity.getId());
            ResponseIntentAdd responseIntentAdd = intentService.add(command);
            return ResponseEntity.ok(responseIntentAdd);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponseIntentAdd.class));
        }
    }

    @PostMapping("/user_id/{userId}")
    public ResponseEntity<ResponseIntents> getByUserId(@PathVariable String userId) {
        try {
            ResponseIntents responseIntents = intentService.getByUserId(userId);
            return ResponseEntity.ok(responseIntents);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponseIntents.class));
        }
    }
}
