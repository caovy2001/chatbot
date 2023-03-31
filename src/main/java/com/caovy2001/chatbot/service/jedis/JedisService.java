package com.caovy2001.chatbot.service.jedis;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class JedisService implements IJedisService {
//    private static Jedis jedis = new Jedis("redis://default:yPqm07QgkiXFbZ9gxR9ejjpmuhO3j9sG@redis-18384.c16.us-east-1-2.ec2.cloud.redislabs.com:18384");
    private static final String jedisConnectionStr = "redis://default:DP52KjeuwAhZyjKrpvP3kBlOurft4mHi@redis-10810.c263.us-east-1-2.ec2.cloud.redislabs.com:10810";
//    private static final Jedis jedis = new Jedis("redis://default:DP52KjeuwAhZyjKrpvP3kBlOurft4mHi@redis-10810.c263.us-east-1-2.ec2.cloud.redislabs.com:10810");

    public static class PrefixRedisKey {
        public static String COLON = ":";
        public static String trainingServerStatus = "training_server_status";

    }
    @Override
    public String get(String key) {
        Jedis jedis = new Jedis(jedisConnectionStr);
        return jedis.get(key);
    }

    @Override
    @Deprecated
    public void set(String key, String value) {
        Jedis jedis = new Jedis(jedisConnectionStr);
        jedis.set(key, value);
    }

    @Override
    public void setWithExpired(String key, String value, long seconds) {
        Jedis jedis = new Jedis(jedisConnectionStr);
        jedis.setex(key, seconds, value);
    }
}
