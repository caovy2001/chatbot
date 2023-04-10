package com.caovy2001.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BaseService implements IBaseService {
    @Override
    public <T extends ResponseBase> T returnException(String exceptionCode, Class<T> clazz) {
        try {
            T entity = clazz.getDeclaredConstructor().newInstance();
            entity.setHttpStatus(HttpStatus.EXPECTATION_FAILED);
            entity.setExceptionCode(exceptionCode);
            return entity;
        } catch (Exception e) {
            log.error(e.toString());
            return null;
        }
    }
}
