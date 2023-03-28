package com.caovy2001.chatbot.service.intent.command;

import com.caovy2001.chatbot.model.DateFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CommandGetListIntent {
    private int page;
    private int size;
    private List<String> ids;
    private String userId;
    private List<String> scriptIds;
    private String keyword;
    private List<String> returnFields;

    @Builder.Default
    private boolean hasPatterns = false;

    public Collection<DateFilter> dateFilters;
}