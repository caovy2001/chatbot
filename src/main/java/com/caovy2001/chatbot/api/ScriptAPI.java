package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.ScriptEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.script.IScriptService;
import com.caovy2001.chatbot.service.script.command.CommandScriptAdd;
import com.caovy2001.chatbot.service.script.command.CommandScriptDelete;
import com.caovy2001.chatbot.service.script.command.CommandScriptUpdate;
import com.caovy2001.chatbot.service.script.response.ResponseScript;
import com.caovy2001.chatbot.service.script.response.ResponseScriptAdd;
import com.caovy2001.chatbot.service.script.response.ResponseScriptGetByUserId;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/script")
public class ScriptAPI {
    @Autowired
    private BaseService baseService;

    @Autowired
    private IScriptService scriptService;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/add")
    public ResponseEntity<ResponseScriptAdd> add(@RequestBody CommandScriptAdd command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            command.setUser_id(userEntity.getId());
            ResponseScriptAdd responseScriptAdd = scriptService.add(command);
            return ResponseEntity.ok(responseScriptAdd);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseScriptAdd.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getScriptById(@PathVariable("id") String id){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            if (StringUtils.isBlank(id)) {
                throw new Exception(ExceptionConstant.missing_param);
            }

            ScriptEntity script = scriptService.getScriptById(id);
            return ResponseEntity.ok(script);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(), ResponseScriptGetByUserId.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_all/by_user_id")
    public ResponseEntity<ResponseScriptGetByUserId> getScriptByUserId(){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseScriptGetByUserId scripts = scriptService.getScriptByUserId(userEntity.getId());
            return ResponseEntity.ok(scripts);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(), ResponseScriptGetByUserId.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_pagination/by_user_id")
    public ResponseEntity<Paginated<ScriptEntity>> getPaginationByUserId(@RequestParam int page, @RequestParam int size){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            page--;
            Paginated<ScriptEntity> scripts = scriptService.getPaginationByUserId(userEntity.getId(), page, size);
            scripts.setPageNumber(++page);
            return ResponseEntity.ok(scripts);
        }
        catch (Exception e){
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 0, 0, 0));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody CommandScriptUpdate command){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            command.setUserId(userEntity.getId());
            ResponseScript script = scriptService.update(command);
            return ResponseEntity.ok(script);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(), ResponseBase.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/delete")
    public  ResponseEntity<?> deleteScriptById(@RequestBody CommandScriptDelete command){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseScript script = scriptService.deleteScript(command.getId());
            return   ResponseEntity.ok(script);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(), ResponseBase.class));
        }
    }



}
