package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.entity.NodeEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.node.INodeService;
import com.caovy2001.chatbot.service.node.command.CommandNodeAdd;
import com.caovy2001.chatbot.service.node.command.CommandNodeAddConditionMapping;
import com.caovy2001.chatbot.service.node.command.CommandNodeDelete;
import com.caovy2001.chatbot.service.node.command.CommandUpdateMessage;
import com.caovy2001.chatbot.service.node.response.ResponseListNode;
import com.caovy2001.chatbot.service.node.response.ResponseNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/node")
public class NodeAPI {
    @Autowired
    private BaseService baseService;
    @Autowired
    private INodeService nodeService;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping
    public  ResponseEntity<?> getNode(@RequestParam(value = "id", required = false) String id, @RequestParam(value = "script_id",required = false) String scriptId){
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }

            ResponseListNode nodes = nodeService.get(id,scriptId);
            return ResponseEntity.ok(nodes);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseNode.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/add")
    public ResponseEntity<ResponseNode> add(@RequestBody CommandNodeAdd command){
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseNode responseNode = nodeService.add(command);
            return ResponseEntity.ok(responseNode);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseNode.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody CommandNodeDelete command){
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseNode responseNode = nodeService.delete(command.getNode_id());
            return ResponseEntity.ok(responseNode);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseNode.class));
        }
    }
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/update_message")
    public ResponseEntity<ResponseNode> updateMessage(@RequestBody CommandUpdateMessage command){
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseNode responseNode = nodeService.updateMessage(command);
            return ResponseEntity.ok(responseNode);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseNode.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/condition_mapping/add")
    public ResponseEntity<ResponseNode> addConditionMapping(@RequestBody CommandNodeAddConditionMapping command){
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseNode node = nodeService.addConditionMapping(command);
            return ResponseEntity.ok(node);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseNode.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/condition_mapping/add_next_node")
    public ResponseEntity<ResponseNode> addNextNode(@RequestBody CommandNodeAddConditionMapping command){
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseNode node = nodeService.addNextNode(command);
            return ResponseEntity.ok(node);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseNode.class));
        }
    }
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/condition_mapping/remove_next_node")
    public ResponseEntity<ResponseNode> removeNextNode(@RequestBody CommandNodeAddConditionMapping command){
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseNode node = nodeService.removeNextNode(command);
            return ResponseEntity.ok(node);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseNode.class));
        }
    }
    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/condition_mapping/update_next_node")
    public ResponseEntity<ResponseNode> updateNextNode(@RequestBody CommandNodeAddConditionMapping command){
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))){
                throw new Exception("auth_invalid");
            }
            ResponseNode node = nodeService.updateNextNode(command);
            return ResponseEntity.ok(node);
        }
        catch (Exception e){
            return ResponseEntity.ok(baseService.returnException(e.getMessage(),ResponseNode.class));
        }
    }

}
