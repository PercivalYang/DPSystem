package com.hmdp;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.impl.UserServiceImpl;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Slf4j
public class HmDianPingApplicationTests {
    @Resource
    private RedissonClient redissonClient;
    private static final Long name = Thread.currentThread().getId();
    private static final String LOCK_PREFIX = "lock:";
    @Resource
    UserServiceImpl userService;
    @Resource
    RedisIdWorker redisIdWorker;
    @Resource
    VoucherOrderServiceImpl voucherOrderService;
    private final ExecutorService es = Executors.newFixedThreadPool(10);

    @Test
    public void testUserDTO() {
        User user = userService.query().eq("phone", "13686869696").one();
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        System.out.println("user = " + user);
    }

    @Test
    public void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        Runnable task = () -> {
            long id = redisIdWorker.nextId("user");
            log.debug("id = " + id);
            latch.countDown();
        };
        LocalDateTime begin = LocalDateTime.now();
        for (int i = 0; i < 10; i++) {
            es.execute(task);
        }
        LocalDateTime end = LocalDateTime.now();
        latch.await();
    }

    @Test
    public void addSekillVoucherOder() {
        Runnable task = () -> {
            voucherOrderService.seckillVoucher(10L);
        };
        for (int i = 0; i < 10; i++) {
            es.execute(task);
        }
    }

    @Test
    public void testSingleTask() {
        List<String> strings = Collections.singletonList(LOCK_PREFIX + name);
        System.out.println(strings);
    }

    @Test
    public void testRedisson() throws InterruptedException {
        RLock lock = redissonClient.getLock("anyLock");
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        if (isLock) {
            try {
                System.out.println("获取锁成功");
            } finally {
                lock.unlock();
            }
        }
    }
}
