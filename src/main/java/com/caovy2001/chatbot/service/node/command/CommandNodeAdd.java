package com.caovy2001.chatbot.service.node.command;

import com.caovy2001.chatbot.entity.ConditionMappingEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommandNodeAdd {
    private String id;
    private String message;
    private String script_id;
    private List<ConditionMappingEntity> condition_mapping;
    private List<Double> position;
}
