package com.caovy2001.chatbot.service.user.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListUser {
    private String id;
    private String secretKey;
    private String username;
    private List<String> returnFields;
}
