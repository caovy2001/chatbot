package com.caovy2001.chatbot.service.training_history.response;

import com.caovy2001.chatbot.entity.TrainingHistoryEntity;
import com.caovy2001.chatbot.service.ResponseBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseTrainingHistory extends ResponseBase {
    private String id;
    private TrainingHistoryEntity.EStatus status;
}
