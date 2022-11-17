package com.caovy2001.chatbot.service.training_history.response;

import com.caovy2001.chatbot.service.ResponseBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseTrainingHistoryAdd extends ResponseBase {
    private String id;
    private String userId;
    private String username;
}
