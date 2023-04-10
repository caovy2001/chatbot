package com.caovy2001.chatbot.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ResponseBase {
    private HttpStatus httpStatus = HttpStatus.OK;
    private String exceptionCode;
}
