package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.PrimitiveIterator;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (!StrUtil.isBlank(shopJson)) {
            // 缓存命中
            log.debug("缓存命中...");
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 缓存未命中，查询数据库
        log.debug("缓存未命中，查询数据库...");
        Shop shop = getById(id);
        if (shop == null) {
            return Result.fail("商铺不存在");
        }
        // 查询后再写入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),30L, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    @Override
    public Result queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (!StrUtil.isBlank(shopJson)) {
            // 缓存命中
            log.debug("缓存命中...");
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        try {
            // 如果没有获取到锁，等待50ms后再次查询
            if (!isLock) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            Shop shop = getById(id);
            if (shop == null) {
                // 防止缓存穿透，将空值写入缓存，TTL为2分钟
                stringRedisTemplate.opsForValue().set(key, "null", 2L, TimeUnit.MINUTES);
                return Result.fail("商铺不存在");
            }
            // 查询后再写入缓存
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),30L, TimeUnit.MINUTES);
            return Result.ok(shop);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }
    }


    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id==null) {
            return Result.fail("商铺id不能为空");
        }
        // 更新数据库
        updateById(shop);
        // 删除缓存
        /*
            为什么选择删除缓存而不是更新缓存？
            减小无效写次数，因为更新后可能不会再被查询，所以删除缓存等下次查询到来时再写入缓存
         */
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    private boolean tryLock(String key) {
        // 将key的互斥锁key添加value，TTL为30，即该线程会在该30s内去数据库更新数据重写到缓存中
        // setIfAbsent对应Redis中的setnx命令，如果key不存在则设置成功，返回true，否则返回false
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 30L, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
