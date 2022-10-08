package com.caovy2001.chatbot.entity;

import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.http.HttpStatus;

@Data
public abstract class BaseEntity {

    @Transient
    private HttpStatus httpStatus = HttpStatus.OK;

    @Transient
    private String exceptionCode;
}
