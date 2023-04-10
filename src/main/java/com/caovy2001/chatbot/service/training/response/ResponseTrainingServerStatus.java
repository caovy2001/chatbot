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
public class ResponseTrainingServerStatus extends ResponseBase {

    private EStatus status;

    public enum EStatus {
        BUSY,
        FREE
    }
}
