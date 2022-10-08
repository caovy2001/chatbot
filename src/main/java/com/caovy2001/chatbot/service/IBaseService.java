package com.caovy2001.chatbot.service;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.caovy2001.chatbot.service.user.response.ResponseBase;

public interface IBaseService {
    <T extends ResponseBase> T returnException(String exceptionCode, Class<T> clazz, String exceptionMessage);
}
