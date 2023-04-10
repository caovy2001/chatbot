package com.caovy2001.chatbot.service.pattern.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseImportExcelStatus {
    private String sessionId;
    private String userId;
    private int numOfSuccess;
    private int numOfFailed;
}
