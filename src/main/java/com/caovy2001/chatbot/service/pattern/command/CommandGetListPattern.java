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
    private String id;
    private String intentId;
    private String keyword;
    private int size = 0;
    private int page = 0;
    private List<DateFilter> dateFilters;
    private List<String> returnFields;
    @Builder.Default
    private boolean hasEntities = false;
    @Builder.Default
    private boolean hasIntentName = false;
    @Builder.Default
    private boolean hasEntityTypeOfEntities = false;
    @Builder.Default
    private Boolean checkPageAndSize = false;
}
