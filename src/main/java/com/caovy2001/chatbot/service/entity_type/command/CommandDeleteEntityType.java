package com.caovy2001.chatbot.service.entity_type.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandDeleteEntityType {
    private String userId;
    private List<String> ids;
    @Builder.Default
    private boolean deleteEntity = false;
}
