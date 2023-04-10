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
public class CommandIntent {
    private String id;
    private String code;
    private String userId;
    private String name;
}
