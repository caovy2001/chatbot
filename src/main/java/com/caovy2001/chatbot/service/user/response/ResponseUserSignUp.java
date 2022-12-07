package com.caovy2001.chatbot.service.user.response;

import com.caovy2001.chatbot.service.ResponseBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseUserSignUp extends ResponseBase {
    private String userId;
    private String username;
    private String token;
    private String secretKey;
}
