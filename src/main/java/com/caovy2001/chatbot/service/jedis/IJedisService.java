package com.caovy2001.chatbot.service.jedis;

public interface IJedisService {
    String get(String key);
    void set(String key, String value);
}
