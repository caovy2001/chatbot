package com.caovy2001.chatbot.service.intent.command;

import com.caovy2001.chatbot.entity.IntentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandIntentAddMany {
    private String userId;
    private List<IntentEntity> intents;
}
