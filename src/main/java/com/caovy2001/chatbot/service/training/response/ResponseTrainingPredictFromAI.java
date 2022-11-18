package com.caovy2001.chatbot.service.training.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseTrainingPredictFromAI {
    private String accuracy;
    private String intentName;
    private String intentId;
}
