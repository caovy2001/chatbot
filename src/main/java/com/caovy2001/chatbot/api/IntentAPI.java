package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.command.CommandIntent;
import com.caovy2001.chatbot.service.intent.command.CommandIntentAddMany;
import com.caovy2001.chatbot.service.intent.command.CommandIntentDelete;
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
    public ResponseEntity<ResponseIntentAdd> add(@RequestBody CommandIntent command) {
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

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/add_many")
    public ResponseEntity<ResponseIntentAdd> addMany(@RequestBody CommandIntentAddMany command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            ResponseIntentAdd responseIntentAdd = intentService.addMany(command);
            return ResponseEntity.ok(responseIntentAdd);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponseIntentAdd.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/user_id")
    public ResponseEntity<ResponseIntents> getByUserId(@RequestBody CommandIntent command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponseIntents responseIntents = intentService.getByUserId(userEntity.getId());
            return ResponseEntity.ok(responseIntents);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponseIntents.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping()
    public ResponseEntity<?> getById(@RequestParam(value = "id", required = false) String id) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponseIntents responseIntents = intentService.getById(id, userEntity.getId());
            return ResponseEntity.ok(responseIntents);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponseIntents.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/update_intent")
    public  ResponseEntity<?> updateName(@RequestBody CommandIntent command){
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");
            ResponseIntents responseIntents = intentService.updateName(command, userEntity.getId());
            return ResponseEntity.ok(responseIntents);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponseIntents.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @DeleteMapping()
    public ResponseEntity<?> delete(@RequestBody CommandIntentDelete command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponseIntents responseIntents = intentService.deleteIntent(command.getId(), userEntity.getId());
            return ResponseEntity.ok(responseIntents);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponseIntents.class));
        }
    }

}
