package com.caovy2001.chatbot.service.common.command;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
public class CommandUpdateBase {
    private String userId;
}
