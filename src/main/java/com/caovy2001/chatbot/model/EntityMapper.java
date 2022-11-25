package com.caovy2001.chatbot.model;

public interface EntityMapper<D, E> {
    D map(E entity);
    void map(E gift, D dto);
}
