package com.caovy2001.chatbot.service.pattern.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseExportExcelStatus {
    private String sessionId;
    private String userId;
    private int numOfSuccess = 0;
    private int numOfFailed = 0;
    private String fileName;
    @Builder.Default
    private EExportExcelStatus status = EExportExcelStatus.PROCESSING;

    public enum EExportExcelStatus {
        PROCESSING,
        DONE
    }
}
