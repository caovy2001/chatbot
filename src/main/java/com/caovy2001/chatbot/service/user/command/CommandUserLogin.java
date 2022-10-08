package com.caovy2001.chatbot.service.user.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandUserLogin {
    private String username;
    private String password;
}
