package com.caovy2001.chatbot.service.intent.response;

import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.service.ResponseBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseIntentAdd extends ResponseBase {
    private String id;
    private List<String> ids;
    private List<IntentEntity> intents;
}
