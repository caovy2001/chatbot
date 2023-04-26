package com.caovy2001.chatbot.service.message_history_group.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListMessageHistoryGroup {
    private int page = 0;
    private int size = 0;
    private String userId;
    private List<String> ids;
    private String sessionId;
    private String scriptId;
    private List<String> returnFields;
    @Builder.Default
    private Boolean hasMessageEntityHistories = false;
}
