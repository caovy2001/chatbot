package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.entity.ScriptEntity;
import com.caovy2001.chatbot.entity.UserEntity;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/script")
public class ScriptAPI {
    @Autowired
    private BaseService baseService;

    @Autowired
    private IScriptService scriptService;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/add")
    public ResponseEntity<ResponseScriptAdd> add(@RequestBody CommandScriptAdd command){
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
    @GetMapping
    public ResponseEntity<?> getScriptById(@RequestParam("id") String id){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ScriptEntity script = scriptService.getScriptById(id);
            return  ResponseEntity.ok(script);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseScriptGetByUserId.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("user_id")
    public ResponseEntity<?> getScriptByUserId(){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseScriptGetByUserId scripts = scriptService.getScriptByUserId(userEntity.getId());
            return  ResponseEntity.ok(scripts);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseBase.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping
    public ResponseEntity<?> updateScriptName(@RequestBody CommandScriptUpdate command){
        try{
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseScript script = scriptService.updateName(command);
            return  ResponseEntity.ok(script);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(), ResponseBase.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @DeleteMapping
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
