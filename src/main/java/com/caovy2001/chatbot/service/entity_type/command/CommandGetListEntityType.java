package com.caovy2001.chatbot.service.entity_type.command;

import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandGetListEntityType extends CommandGetListBase {
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
