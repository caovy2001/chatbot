package com.caovy2001.chatbot.service.entity.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListEntity {
    private int page;
    private int size;
    private String userId;
    private List<String> ids;
    private String keyword;
    private String patternId;
    private List<String> entityTypeIds;
    private List<String> returnFields;
    @Builder.Default
    public boolean hasEntityType = false;
    @Builder.Default
    public boolean hasPattern = false;
}
