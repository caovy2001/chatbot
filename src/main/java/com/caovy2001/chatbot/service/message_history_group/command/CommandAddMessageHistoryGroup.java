package com.caovy2001.chatbot.service.message_history_group.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddMessageHistoryGroup {
    private String userId;
    private String sessionId;
    private String scriptId;
    @Builder.Default
    private Long createdDate = System.currentTimeMillis();
}
