package com.caovy2001.chatbot.service.training_history.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandTrainingHistoryAdd {
    private String userId;
    private String username;
}
