package com.caovy2001.chatbot.service.script.command;

import com.caovy2001.chatbot.entity.NodeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandScriptUpdate {
    private String id;
    private String name;
    private String userId;
    private String wrongMessage;
    private String endMessage;
    private List<NodeEntity> nodes;
    private Map<String, Object> uiRendering;
}
