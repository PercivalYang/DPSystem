package com.hmdp.service;

public interface ILock {
    /**
     * 加锁
     *
     * @param expireTime 锁的过期时间
     * @return 是否加锁成功
     */
    boolean trylock(long expireTime);

    /**
     * 解锁
     *
     * @return 是否解锁成功
     */
    void unlock();
}
