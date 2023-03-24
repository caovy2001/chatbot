package com.caovy2001.chatbot.service.jedis;

public interface IJedisService {
    String get(String key);
    @Deprecated
    void set(String key, String value);
    void setWithExpired(String key, String value, long seconds);
}
