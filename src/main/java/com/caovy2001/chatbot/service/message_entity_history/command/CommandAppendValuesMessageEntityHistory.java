package com.caovy2001.chatbot.service.message_entity_history.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandAppendValuesMessageEntityHistory {
    private CommandGetListMessageEntityHistory commandGet;
    private List<String> values;
    @Builder.Default
    private boolean addWhenGetNull = false;
}
