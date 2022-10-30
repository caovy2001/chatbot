package com.caovy2001.chatbot.service;

public interface IBaseService {
    <T extends ResponseBase> T returnException(String exceptionCode, Class<T> clazz);
}
