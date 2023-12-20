package com.caovy2001.chatbot.service.training.command;

import com.caovy2001.chatbot.entity.NodeEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandCheckConditionByConditionMapping {
    private String message;
    private UserEntity user;
    private NodeEntity currentNode;
    private String sessionId;

}
