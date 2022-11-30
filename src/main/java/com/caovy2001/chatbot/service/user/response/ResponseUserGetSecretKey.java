package com.caovy2001.chatbot.service.user.response;

import com.caovy2001.chatbot.service.ResponseBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseUserGetSecretKey extends ResponseBase {
    private String userId;
    private String secretKey;
}
