package com.caovy2001.chatbot.service.pattern.command;

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
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetListPattern extends CommandGetListBase {
//    private String userId;
    private String id;
    private List<String> ids;
    private String intentId;
    private List<String> intentIds;
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
    private Boolean hasEntityTypeOfEntities = false;
    @Builder.Default
    private Boolean checkPageAndSize = false;
}
