package com.caovy2001.chatbot.service.jedis;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class JedisService implements IJedisService {
    private static Jedis jedis = new Jedis("redis://default:yPqm07QgkiXFbZ9gxR9ejjpmuhO3j9sG@redis-18384.c16.us-east-1-2.ec2.cloud.redislabs.com:18384");

    @Override
    public String get(String key) {
        return jedis.get(key);
    }

    @Override
    public void set(String key, String value) {
        jedis.set(key, value);
    }
}
