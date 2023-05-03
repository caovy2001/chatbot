package com.caovy2001.chatbot.service.common.command;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class CommandAddBase {
    private String userId;
}
