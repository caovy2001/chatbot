package com.caovy2001.chatbot.service.entity_type.command;

import com.caovy2001.chatbot.entity.EntityTypeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CommandEntityTypeUpdateManyByQuery {
    private String userId;
    private CommandGetListEntityType commandGetListEntityType;
    private List<String> fieldsToUpdate; // Tên trường ở dạng lạc đà
    private EntityTypeEntity value;
}
