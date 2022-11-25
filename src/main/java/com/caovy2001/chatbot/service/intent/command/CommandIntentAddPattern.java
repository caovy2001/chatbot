package com.caovy2001.chatbot.service.intent.command;

import com.caovy2001.chatbot.entity.PatternEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommandIntentAddPattern {
    private String userId;
    private String intentId;
    private List<PatternEntity> patterns;
}
