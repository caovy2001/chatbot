package com.caovy2001.chatbot.service.intent.command;

import com.caovy2001.chatbot.entity.IntentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandIndexingIntentES {
    private String userId;
    private List<IntentEntity> intents;
    @Builder.Default
    private boolean doSetUserId = true;
}
