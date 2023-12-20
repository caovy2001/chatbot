package com.caovy2001.chatbot.service.training.command;

import com.caovy2001.chatbot.service.training.response.ResponseTrainingPredictFromAI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseCheckConditionByConditionMapping {
    private List<String> nextNodeIds;
    private ResponseTrainingPredictFromAI responseTrainingPredictFromAI;
}
