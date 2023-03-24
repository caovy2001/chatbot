package com.caovy2001.chatbot.service.entity_type.command;

import com.caovy2001.chatbot.entity.EntityTypeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandEntityTypeAddMany {
    private String userId;
    private List<EntityTypeEntity> entityTypes;
}
