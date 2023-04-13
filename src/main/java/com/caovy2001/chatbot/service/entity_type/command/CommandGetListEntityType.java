package com.caovy2001.chatbot.service.entity_type.command;

import com.caovy2001.chatbot.model.DateFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandGetListEntityType {
    private String userId;
    private int size = 0;
    private int page = 0;
    private String keyword;
    private String id;
    private List<String> ids;
    private String lowerCaseName;
    private List<String> lowerCaseNames;
    private List<String> returnFields;
    @Builder.Default
    private boolean hasEntities = false;
    @Builder.Default
    private boolean hasPatternOfEntities = false;
    public List<DateFilter> dateFilters;
}
