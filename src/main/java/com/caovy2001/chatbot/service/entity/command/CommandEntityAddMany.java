package com.caovy2001.chatbot.service.entity.command;

import com.caovy2001.chatbot.entity.EntityEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandEntityAddMany {
    private String userId;
    private List<EntityEntity> entities;
}
