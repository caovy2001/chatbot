package com.caovy2001.chatbot.service.script.command;

import com.caovy2001.chatbot.entity.NodeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandScriptAdd {
    private String user_id;
    private String name;
    private List<NodeEntity> nodes;
}
