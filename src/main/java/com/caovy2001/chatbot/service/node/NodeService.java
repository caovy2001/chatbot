package com.caovy2001.chatbot.service.node;

import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.NodeEntity;
import com.caovy2001.chatbot.repository.NodeRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.node.command.CommandNodeAdd;
import com.caovy2001.chatbot.service.node.command.CommandNodeAddConditionMapping;
import com.caovy2001.chatbot.service.node.command.CommandUpdateMessage;
import com.caovy2001.chatbot.service.node.response.ResponseListNode;
import com.caovy2001.chatbot.service.node.response.ResponseNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NodeService extends BaseService implements INodeService{
    @Autowired
    private NodeRepository nodeRepository;

    @Override
    public ResponseNode add(CommandNodeAdd command) {
        if (StringUtils.isAnyBlank(command.getMessage(), command.getScript_id())) {
            return returnException(ExceptionConstant.missing_param, ResponseNode.class);
        }

        NodeEntity nodeEntity = NodeEntity.builder()
                .message(command.getMessage())
                .position(command.getPosition())
                .conditionMappings(command.getCondition_mapping())
                .scriptId(command.getScript_id()).build();

        return ResponseNode.builder().nodes(nodeRepository.insert(nodeEntity)).build();
    }

    @Override
    public ResponseListNode get(String id, String scriptId) {
        List<NodeEntity> nodes = new ArrayList<NodeEntity>();
        if (id != null){
            NodeEntity node = nodeRepository.findById(id).orElse(null);
            nodes.add(node);
            return ResponseListNode.builder().nodes(nodes).build();
        }
        else if (scriptId != null){
            nodes = nodeRepository.findByScriptId(scriptId);
            return  ResponseListNode.builder().nodes(nodes).build();
        }
        else {
            return returnException(ExceptionConstant.missing_param, ResponseListNode.class);
        }
    }

    @Override
    public ResponseNode updateMessage(CommandUpdateMessage command) {
        if (command.getMessage() == null){
            return  returnException(ExceptionConstant.missing_param, ResponseNode.class);
        }
        NodeEntity node = nodeRepository.findById(command.getNode_id()).orElse(null);
        node.setMessage(command.getMessage());
        return ResponseNode.builder().nodes(nodeRepository.save(node)).build();
    }

    @Override
    public ResponseNode delete(String id) {
        if (id == null){
            return  returnException(ExceptionConstant.missing_param, ResponseNode.class);
        }
        NodeEntity node = nodeRepository.findById(id).orElse(null);
        if (node == null){
            return  returnException(ExceptionConstant.item_not_found,ResponseNode.class);
        }
        nodeRepository.deleteById(id);
        return ResponseNode.builder().build();
    }

    @Override
    public void deleteMany(List<String> ids) {
        nodeRepository.deleteAllById(ids);
    }

    @Override
    public List<NodeEntity> getAllByScriptId(String scriptId) {
        return nodeRepository.findByScriptId(scriptId);
    }

    @Override
    public List<NodeEntity> addMany(List<NodeEntity> nodes) {
        return nodeRepository.insert(nodes);
    }

    @Override
    public ResponseNode addConditionMapping(CommandNodeAddConditionMapping command) {
        if (command.getCondition_mapping() == null || command.getNode_id() == null){
            return  returnException(ExceptionConstant.missing_param, ResponseNode.class);
        }
        NodeEntity node = nodeRepository.findById(command.getNode_id()).orElse(null);
        if (node == null){
            return  returnException(ExceptionConstant.item_not_found, ResponseNode.class);
        }
        node.getConditionMappings().add(command.getCondition_mapping());
        return ResponseNode.builder().nodes(nodeRepository.save(node)).build();
    }

    @Override
    public ResponseNode addNextNode(CommandNodeAddConditionMapping command) {
        if (command.getNew_node_id() == null){
            return  returnException(ExceptionConstant.missing_param, ResponseNode.class);
        }
        NodeEntity node = nodeRepository.findById(command.getNode_id()).orElse(null);
        node.getConditionMappings()
                .stream().
                filter(p-> p.getId().equals(command.getCondition_mapping_id())).collect(Collectors.toList())
                .get(0)
                .getNext_node_ids()
                .add(command.getNew_node_id());
        return ResponseNode.builder().nodes(nodeRepository.save(node)).build();
    }

    @Override
    public ResponseNode removeNextNode(CommandNodeAddConditionMapping command) {
        if (command.getRemove_node_id() == null){
            return  returnException(ExceptionConstant.missing_param, ResponseNode.class);
        }
        NodeEntity node = nodeRepository.findById(command.getNode_id()).orElse(null);
        node.getConditionMappings()
                .stream().
                filter(p-> p.getId().equals(command.getCondition_mapping_id())).collect(Collectors.toList())
                .get(0)
                .getNext_node_ids()
                .removeIf(predicate -> predicate.equals(command.getRemove_node_id()));
        return ResponseNode.builder().nodes(nodeRepository.save(node)).build();
    }
    @Override
    public ResponseNode updateNextNode(CommandNodeAddConditionMapping command) {
        if (command.getOld_node_id() == null || command.getNew_node_id() == null){
            return  returnException(ExceptionConstant.missing_param, ResponseNode.class);
        }
        NodeEntity node = nodeRepository.findById(command.getNode_id()).orElse(null);
        node.getConditionMappings()
                .stream().
                filter(p-> p.getId().equals(command.getCondition_mapping_id())).collect(Collectors.toList())
                .get(0)
                .getNext_node_ids()
                .replaceAll(s-> s.equals(command.getOld_node_id())?command.getNew_node_id():s);
        return ResponseNode.builder().nodes(nodeRepository.save(node)).build();
    }

}
