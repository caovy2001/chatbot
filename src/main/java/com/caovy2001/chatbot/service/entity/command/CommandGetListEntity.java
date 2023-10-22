package com.caovy2001.chatbot.service.entity.command;

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
public class CommandGetListEntity extends CommandGetListBase {
    private int page;
    private int size;
//    private String userId;
    private List<String> ids;
    private String keyword;
    private String patternId;
    private List<String> patternIds;
    private List<String> entityTypeIds;
    private List<String> returnFields;
    @Builder.Default
    public boolean hasEntityType = false;
    @Builder.Default
    public boolean hasPattern = false;
}
