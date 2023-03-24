package com.caovy2001.chatbot.service.entity_type.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private List<String> ids;
    private List<String> returnFields;
}
