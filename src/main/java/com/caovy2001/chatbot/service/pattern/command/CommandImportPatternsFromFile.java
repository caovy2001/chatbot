package com.caovy2001.chatbot.service.pattern.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandImportPatternsFromFile {
    private String userId;
    private String sessionId;
    private String extensionType;

}
