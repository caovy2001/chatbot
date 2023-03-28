package com.caovy2001.chatbot.service.script.command;

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
public class CommandGetListScript {
    private int page = 0;
    private int size = 0;
    private String userId;
    private String keyword;
    private List<String> ids;
    private List<DateFilter> dateFilters;
    @Builder.Default
    private boolean hasNodes = false;
    private List<String> returnFields;
}
