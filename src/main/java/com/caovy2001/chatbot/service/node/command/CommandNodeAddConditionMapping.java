package com.caovy2001.chatbot.service.node.command;

import com.caovy2001.chatbot.entity.ConditionMappingEntity;
import com.caovy2001.chatbot.repository.ConditionMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommandNodeAddConditionMapping {
    private String node_id;
    private String condition_mapping_id;
    private String new_node_id;
    private String remove_node_id;
    private String old_node_id;
    private ConditionMappingEntity condition_mapping;
}
