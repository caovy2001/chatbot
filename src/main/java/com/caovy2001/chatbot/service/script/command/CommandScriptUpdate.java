package com.caovy2001.chatbot.service.script.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandScriptUpdate {
    private String id;
    private  String name;
}
