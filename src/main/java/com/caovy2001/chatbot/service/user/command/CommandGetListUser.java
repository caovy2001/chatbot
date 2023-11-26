package com.caovy2001.chatbot.service.user.command;

import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.user.enumeration.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListUser extends CommandGetListBase {
    private String id;
    private String secretKey;
    private String username;
    private String keyword;
    private UserRole role;
    private List<String> returnFields;
}
