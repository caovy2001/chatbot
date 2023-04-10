package com.caovy2001.chatbot.service.training.command;

import com.caovy2001.chatbot.service.training.response.ResponseTrainingPredictFromAI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseCheckConditionByConditionMapping {
    private String nextNodeId;
    private ResponseTrainingPredictFromAI responseTrainingPredictFromAI;
}
