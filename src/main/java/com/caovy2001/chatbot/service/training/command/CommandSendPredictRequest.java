package com.caovy2001.chatbot.service.training.command;

import com.caovy2001.chatbot.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandSendPredictRequest {
    private String message;
    private UserEntity user;
    private List<String> intentIds;
    private String sessionId;
}
