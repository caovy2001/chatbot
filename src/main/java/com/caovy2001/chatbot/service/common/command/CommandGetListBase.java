package com.caovy2001.chatbot.service.common.command;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.query.Query;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class CommandGetListBase {
    protected String userId;
    protected int page = 0;
    protected int size = 0;
    protected Query query;
    protected boolean checkPageAndSize = false;
}
