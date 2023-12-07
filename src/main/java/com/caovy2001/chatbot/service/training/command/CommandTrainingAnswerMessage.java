package com.caovy2001.chatbot.service.training.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandTrainingAnswerMessage {
    private String userId;
    private String currentNodeId;
    private String message;
    private String sessionId;
    private String scriptId;

}
