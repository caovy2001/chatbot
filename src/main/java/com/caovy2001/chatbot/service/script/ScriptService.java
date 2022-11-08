package com.caovy2001.chatbot.service.script;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.ScriptEntity;
import com.caovy2001.chatbot.repository.ScriptRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.script.command.CommandScriptAdd;
import com.caovy2001.chatbot.service.script.command.CommandScriptDelete;
import com.caovy2001.chatbot.service.script.command.CommandScriptUpdate;
import com.caovy2001.chatbot.service.script.response.ResponseScriptAdd;
import com.caovy2001.chatbot.service.script.response.ResponseScriptGetByUserId;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScriptService extends BaseService implements IScriptService {
    @Autowired
    private ScriptRepository scriptRepository;
    
    @Override
    public ResponseScriptAdd add(CommandScriptAdd command) {
        if (StringUtils.isAnyBlank(command.getUser_id(),command.getName())){
            return  returnException(ExceptionConstant.missing_param,ResponseScriptAdd.class);
        }

        ScriptEntity script = ScriptEntity.builder()
                .name(command.getName())
                .userId((command.getUser_id()))
                .build();
        ScriptEntity addedScript = scriptRepository.insert(script);
        return ResponseScriptAdd.builder()
                .id(addedScript.getId())
                .build();
    }

    @Override
    public ResponseScriptGetByUserId getScriptByUserId(String userId) {
        if (StringUtils.isAnyBlank(userId)){
            return  returnException(ExceptionConstant.missing_param, ResponseScriptGetByUserId.class);
        }
        return ResponseScriptGetByUserId.builder()
                .scripts(scriptRepository.findByUserId(userId))
                .build();
    }

    @Override
    public ScriptEntity getScriptById(String id) {
        return scriptRepository.findById(id).orElse(null);
    }
    @Override
    public ScriptEntity updateName(CommandScriptUpdate command) {
        ScriptEntity script = scriptRepository.findById(command.getId()).orElse(null);
        script.setName(command.getName());
        return scriptRepository.save(script);
    }

    @Override
    public void  deleteScript(String id) {
        scriptRepository.deleteById(id);
    }
}
