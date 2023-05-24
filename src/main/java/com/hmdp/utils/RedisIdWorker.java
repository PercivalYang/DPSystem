package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMESTAMP = 1684938541L;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final int COUNT_BITS = 32;


    public long nextId(String keyPrefix) {
        // 生成高31位的时间戳
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timestamp = now - BEGIN_TIMESTAMP;

        // 生成低32位的序列号
        // 1. 获取当前日期，通过":"分开是为了适配Redis分层符号
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2. 自增长
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        //
        return timestamp << COUNT_BITS | count;
    }

}
