package com.caovy2001.chatbot.service.training.command;

import com.caovy2001.chatbot.entity.IntentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandTrainingTrain {
    private String userId;
    private String username;
    private String trainingHistoryId;
    private List<IntentEntity> intents;
}
