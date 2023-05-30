package com.hmdp.service.impl;

import com.hmdp.service.ILock;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    StringRedisTemplate stringRedisTemplate;
    private static final String LOCK_PREFIX = "lock:";
    private String name;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean trylock(long expireTime) {
        String value = String.valueOf(Thread.currentThread().getId());
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + name, value, expireTime, TimeUnit.SECONDS);
        // 防止自动拆包问题
        return Boolean.TRUE.equals(success);
    }

    @Override
    public boolean unlock() {
        return stringRedisTemplate.delete(LOCK_PREFIX + name);
    }
}
