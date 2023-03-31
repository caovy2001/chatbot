package com.caovy2001.chatbot.service.entity.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListEntity {
    private String userId;

    private String keyword;
    private String patternId;

    @Builder.Default
    public boolean hasEntityType = false;
}
