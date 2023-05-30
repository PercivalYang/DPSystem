--KEYS[1]: 锁的key
--ARGV[1]: 当前线程的Id
if (redis.call('GET', KEYS[1]) == ARGV[1]) then
    return redis.call('DEL', KEYS[1])
else
    return 0
end