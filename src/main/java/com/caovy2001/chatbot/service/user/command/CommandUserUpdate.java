package com.caovy2001.chatbot.service.user.command;

import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.user.enumeration.UserServicePack;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandUserUpdate extends CommandUpdateBase {
    private String userId;
    private String zaloGroupLink;
    private String googleMeetLink;
    private UserServicePack currentServicePack;
}
