package com.caovy2001.chatbot.service.intent.command;

import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CommandGetListIntent extends CommandGetListBase {
    private int page;
    private int size;
    private List<String> ids;
//    private String userId;
    private String code;
    private List<String> codes;
    private List<String> scriptIds;
    private String keyword;
    private List<String> returnFields;

    @Builder.Default
    private Boolean checkPageAndSize = false;
    @Builder.Default
    private boolean hasPatterns = false;
    @Builder.Default
    private boolean hasEntitiesOfPatterns = false;
    @Builder.Default
    private boolean hasEntityTypesOfEntitiesOfPatterns = false;

    public List<DateFilter> dateFilters;
}
