package com.caovy2001.chatbot.service.entity.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandDeleteEntity {
    private String userId;
    private List<String> ids;
    private List<String> entityType;
}
