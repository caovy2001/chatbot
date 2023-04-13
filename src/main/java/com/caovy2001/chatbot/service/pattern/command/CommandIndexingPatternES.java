package com.caovy2001.chatbot.service.pattern.command;

import com.caovy2001.chatbot.entity.PatternEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandIndexingPatternES {
    private String userId;
    private List<PatternEntity> patterns;
    @Builder.Default
    private boolean doSetUserId = true;
}
