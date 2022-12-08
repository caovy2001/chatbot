package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.entity.ScriptEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.intent.response.ResponseIntentAdd;
import com.caovy2001.chatbot.service.node.command.CommandNodeDelete;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.CommandPattern;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternAdd;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternDelete;
import com.caovy2001.chatbot.service.pattern.command.CommandPatternUpdate;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

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
    @PostMapping("/update")
    public ResponseEntity<ResponsePattern> update(@RequestBody CommandPatternUpdate command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            if (command == null || StringUtils.isBlank(command.getId())) {
                throw new Exception(ExceptionConstant.missing_param);
            }

            command.setUserId(userEntity.getId());
            ResponsePattern responsePattern = patternService.update(command);
            return ResponseEntity.ok(responsePattern);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
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
    @PostMapping("/delete")
    public ResponseEntity<ResponsePattern> delete(@RequestBody CommandPatternDelete command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            ResponsePattern responsePattern = patternService.delete(command);
            return ResponseEntity.ok(responsePattern);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_pagination/by_user_id")
    public ResponseEntity<Paginated<PatternEntity>> getPaginationByUserId(@RequestParam int page, @RequestParam int size){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            page--;
            Paginated<PatternEntity> patterns = patternService.getPaginationByUserId(userEntity.getId(), page, size);
            patterns.setPageNumber(++page);
            return ResponseEntity.ok(patterns);
        }
        catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 0, 0, 0));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_pagination/by_intent_id/{intent_id}")
    public ResponseEntity<Paginated<PatternEntity>> getPaginationByIntentId(@RequestParam int page, @RequestParam int size, @PathVariable String intent_id){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            page--;
            Paginated<PatternEntity> patterns = patternService.getPaginationByIntentId(intent_id, page, size);
            patterns.setPageNumber(++page);
            return ResponseEntity.ok(patterns);
        }
        catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 0, 0, 0));
        }
    }
}
