package com.caovy2001.chatbot.service.script_intent_mapping.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandGetListScriptIntentMapping {
    private String userId;
    private List<String> scriptIds;
    private List<String> returnFields;
}
