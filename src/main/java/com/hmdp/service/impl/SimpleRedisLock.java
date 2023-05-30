package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hmdp.service.ILock;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    StringRedisTemplate stringRedisTemplate;
    private static final String LOCK_PREFIX = "lock:";
    // UUID的作用是集成环境下，多个JVM可能会产生相同的线程ID
    // UUID避免只用线程ID时导致误删的情况
    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";
    private String name;
    private String value;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean trylock(long expireTime) {
        value = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + name, value, expireTime, TimeUnit.SECONDS);
        // 防止自动拆包问题
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        String s = stringRedisTemplate.opsForValue().get(LOCK_PREFIX + name);
        if (s.equals(value))
            stringRedisTemplate.delete(LOCK_PREFIX + name);
    }
}
