package com.caovy2001.chatbot.service.script;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.NodeEntity;
import com.caovy2001.chatbot.entity.ScriptEntity;
import com.caovy2001.chatbot.repository.ScriptRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.node.INodeService;
import com.caovy2001.chatbot.service.script.command.CommandScriptAdd;
import com.caovy2001.chatbot.service.script.command.CommandScriptUpdate;
import com.caovy2001.chatbot.service.script.response.ResponseScript;
import com.caovy2001.chatbot.service.script.response.ResponseScriptAdd;
import com.caovy2001.chatbot.service.script.response.ResponseScriptGetByUserId;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScriptService extends BaseService implements IScriptService {
    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private INodeService nodeService;
    
    @Override
    public ResponseScriptAdd add(CommandScriptAdd command) {
        if (StringUtils.isAnyBlank(command.getUser_id(),command.getName())){
            return returnException(ExceptionConstant.missing_param, ResponseScriptAdd.class);
        }

        if (CollectionUtils.isEmpty(command.getNodes())) {
            return returnException("List_nodes_empty", ResponseScriptAdd.class);
        }

        ScriptEntity script = ScriptEntity.builder()
                .name(command.getName())
                .userId((command.getUser_id()))
                .uiRendering(command.getUiRendering())
                .build();
        ScriptEntity addedScript = scriptRepository.insert(script);

        for (NodeEntity node: command.getNodes()) {
            node.setScriptId(script.getId());
        }
        List<NodeEntity> addedNodes = nodeService.addMany(command.getNodes());
        if (CollectionUtils.isEmpty(addedNodes)) {
            scriptRepository.deleteById(script.getId());
            return returnException("Add_nodes_fail", ResponseScriptAdd.class);
        }
        addedScript.setNodes(addedNodes);

        return ResponseScriptAdd.builder()
                .script(addedScript)
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
        ScriptEntity script = scriptRepository.findById(id).orElse(null);
        if (script == null) {
            return null;
        }

        // Lay cac node cua script nay
        List<NodeEntity> nodes = nodeService.getAllByScriptId(script.getId());
        if (!CollectionUtils.isEmpty(nodes)) {
            script.setNodes(nodes);
        }

        return script;
    }
    @Override
    public ResponseScript updateName(CommandScriptUpdate command) {
        ScriptEntity script = scriptRepository.findById(command.getId()).orElse(null);
        if (script == null){
            return returnException(ExceptionConstant.item_not_found, ResponseScript.class);
        }
        script.setName(command.getName());
        return ResponseScript.builder().script(scriptRepository.save(script)).build();
    }

    @Override
    public ResponseScript  deleteScript(String id) {
        scriptRepository.deleteById(id);

        List<NodeEntity> nodes = nodeService.getAllByScriptId(id);
        if (!CollectionUtils.isEmpty(nodes)) {
            nodeService.deleteMany(nodes.stream().map(NodeEntity::getId).collect(Collectors.toList()));
        }

        return ResponseScript.builder()
                .script(ScriptEntity.builder()
                        .id(id)
                        .build())
                .build();
    }

    @Override
    public ResponseScript update(CommandScriptUpdate command) {
        if (StringUtils.isAnyEmpty(command.getId(), command.getUser_id()) ||
        CollectionUtils.isEmpty(command.getNodes())) {
            return returnException(ExceptionConstant.missing_param, ResponseScript.class);
        }

        ScriptEntity existScript = scriptRepository.findById(command.getId()).orElse(null);
        if (existScript == null) {
            return returnException("script_null", ResponseScript.class);
        }

        List<NodeEntity> oldNodes = nodeService.getAllByScriptId(existScript.getId());

        for (NodeEntity node: command.getNodes()) {
            node.setScriptId(existScript.getId());
        }
        List<NodeEntity> addedNodes = nodeService.addMany(command.getNodes());

        if (!CollectionUtils.isEmpty(addedNodes) && !CollectionUtils.isEmpty(oldNodes)) {
            nodeService.deleteMany(oldNodes.stream().map(NodeEntity::getId).collect(Collectors.toList()));
        }

        if (CollectionUtils.isEmpty(addedNodes)) {
            return returnException("Add_nodes_fail", ResponseScript.class);
        }

        existScript.setName(command.getName());
        existScript.setUserId(command.getUser_id());
        existScript.setUiRendering(command.getUi_rendering());
        ScriptEntity updatedScript = scriptRepository.save(existScript);
        updatedScript.setNodes(addedNodes);
        return ResponseScript.builder()
                .script(updatedScript)
                .build();
    }
}
