package com.caovy2001.chatbot.service.training_history.command;

import com.caovy2001.chatbot.entity.TrainingHistoryEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandTrainingHistory {
    private String id;
    private String userId;
    private TrainingHistoryEntity.EStatus status;
}
