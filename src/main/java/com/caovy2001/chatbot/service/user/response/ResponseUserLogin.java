package com.caovy2001.chatbot.service.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseUserLogin extends ResponseBase {
    private String username;
    private String token;
}
