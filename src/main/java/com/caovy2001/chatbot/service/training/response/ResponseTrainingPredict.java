package com.caovy2001.chatbot.service.training.response;

import com.caovy2001.chatbot.service.ResponseBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseTrainingPredict extends ResponseBase {
    private String currentNodeId;
    private String message;
}
