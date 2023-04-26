package com.caovy2001.chatbot.service.message_history.command;

import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.enumeration.EMessageHistoryFrom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddMessageHistory {
    private String userId;
    private String scriptId;
    private String nodeId;
    private String sessionId;
    private String message;
    private EMessageHistoryFrom from;
    private List<EntityEntity> entities;
    @Builder.Default
    private Long createdDate = System.currentTimeMillis();
    @Builder.Default
    private boolean checkAddMessageHistoryGroup = false; // Check xem đã có lưu message history group theo session id này hay chưa
    @Builder.Default
    private boolean saveMessageEntityHistory= false;
}
