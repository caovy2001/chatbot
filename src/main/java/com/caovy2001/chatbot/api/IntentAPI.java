package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.command.*;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/intent")
public class IntentAPI {
    @Autowired
    private IIntentService intentService;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/add")
    public ResponseEntity<ResponseIntentAdd> add(@RequestBody CommandIntentAdd command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            IntentEntity intent = intentService.add(command);
            if (intent == null) {
                throw new Exception(ExceptionConstant.error_occur);
            }

            return ResponseEntity.ok(ResponseIntentAdd.builder()
                    .id(intent.getId())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(intentService.returnException(e.getMessage(), ResponseIntentAdd.class));
        }
    }

//    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
//    @PostMapping("/add_many")
//    public ResponseEntity<ResponseIntentAdd> addMany(@RequestBody CommandIntentAddMany command) {
//        try {
//            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
//                throw new Exception("auth_invalid");
//
//            command.setUserId(userEntity.getId());
//            List<IntentEntity> intents = intentService.add(command);
//            if (CollectionUtils.isEmpty(intents)) {
//                throw new Exception(ExceptionConstant.error_occur);
//            }
//
//            return ResponseEntity.ok(ResponseIntentAdd.builder()
//                    .ids(intents.stream().map(IntentEntity::getId).toList())
//                    .build());
//        } catch (Exception e) {
//            return ResponseEntity.ok(intentService.returnException(e.getMessage(), ResponseIntentAdd.class));
//        }
//    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_all/by_user_id")
    public ResponseEntity<ResponseIntents> getByUserId() {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            return ResponseEntity.ok(ResponseIntents.builder()
                    .intents(intentService.getList(CommandGetListIntent.builder()
                            .userId(userEntity.getId())
                            .build(), IntentEntity.class))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(intentService.returnException(e.toString(), ResponseIntents.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/get_pagination")
    public ResponseEntity<Paginated<IntentEntity>> getByPagination(@RequestBody CommandGetListIntent command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            return ResponseEntity.ok(intentService.getPaginatedList(command, IntentEntity.class, CommandGetListIntent.class));
        } catch (Exception e) {
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 1, 0, 0));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get/{id}")
    public ResponseEntity<ResponseIntents> getById(@PathVariable String id) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            List<IntentEntity> intents = intentService.getList(CommandGetListIntent.builder()
                    .userId(userEntity.getId())
                    .ids(List.of(id))
                    .hasPatterns(true)
                    .build(), IntentEntity.class);

            if (CollectionUtils.isEmpty(intents)) {
                throw new Exception("intent_null");
            }

            return ResponseEntity.ok(ResponseIntents.builder()
                    .intent(intents.get(0))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(intentService.returnException(e.getMessage(), ResponseIntents.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody CommandIntentUpdate command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            return ResponseEntity.ok(ResponseIntents.builder()
                    .intent(intentService.update(command))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(intentService.returnException(e.getMessage(), ResponseIntents.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody CommandIntentDelete command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            if (BooleanUtils.isFalse(intentService.delete(CommandGetListIntent.builder()
                    .userId(userEntity.getId())
                    .ids(List.of(command.getId()))
                    .build()))) {
                throw new Exception(ExceptionConstant.error_occur);
            }

            return ResponseEntity.ok(ResponseIntents.builder().build());
        } catch (Exception e) {
            return ResponseEntity.ok(intentService.returnException(e.getMessage(), ResponseIntents.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/suggest_pattern")
    public ResponseEntity<?> suggestPattern(@RequestBody CommandIntentSuggestPattern command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            if (BooleanUtils.isFalse(intentService.suggestPattern(command))) {
                throw new Exception(ExceptionConstant.error_occur);
            }

            return ResponseEntity.ok(ResponseIntents.builder().build());
        } catch (Exception e) {
            return ResponseEntity.ok(intentService.returnException(e.getMessage(), ResponseIntents.class));
        }
    }
}
