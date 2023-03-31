package com.caovy2001.chatbot.service.pattern.command;

import com.caovy2001.chatbot.model.DateFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListPattern {
    private String userId;
    private String intentId;
    private String keyword;
    private int size = 0;
    private int page = 0;
    private List<DateFilter> dateFilters;
    @Builder.Default
    private boolean hasEntities = false;
    @Builder.Default
    private boolean hasEntityTypeOfEntities = false;
}
