package com.caovy2001.chatbot.service.intent.response;

import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.service.ResponseBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseIntents extends ResponseBase {
    private List<IntentEntity> intents;
    private IntentEntity intent;
    private List<PatternEntity> patterns;
}
