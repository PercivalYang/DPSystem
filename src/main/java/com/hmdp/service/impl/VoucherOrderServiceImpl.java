package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private SeckillVoucherServiceImpl seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher svoucher = seckillVoucherService.getById(voucherId);
        // 秒杀活动还没开始
        if (svoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            Result.fail("秒杀活动尚未开始！");
        }
        // 秒杀活动已经结束
        if (svoucher.getEndTime().isBefore(LocalDateTime.now())) {
            Result.fail("秒杀活动已经结束！");
        }
        // 检查库存剩余
        if (svoucher.getStock() < 1) {
            Result.fail("库存不足！");
        }
        // 库存余量足够，创建订单
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock-1")
                .eq("voucher_id", voucherId)
                // 保证库存大于0，避免超卖
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足！");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        // 设置订单Id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 设置用户Id
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        // 设置优惠券Id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
