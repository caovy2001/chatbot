package com.caovy2001.chatbot.service.training.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandTrainingPredict {
    private String userId;
    private String secretKey;
    private String scriptId;
    private String currentNodeId;
    private String message;
    private String sessionId;
    @Builder.Default
    private Boolean isTrying = false;
}
