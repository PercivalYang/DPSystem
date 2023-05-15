package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // Session 版本
        // 判断手机号格式是否符合要求
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 生成验证码，利用hutool的RandomUtil工具类
        String code = RandomUtil.randomNumbers(6);
        // 将验证码保存到Redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code);
//        // 保存验证码到session
//        session.setAttribute("code", code);
        log.debug("发送验证码成功，验证码: {}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
//        // Session 版本
//        // 校验手机号
//        String phone = loginForm.getPhone();
//        if (RegexUtils.isPhoneInvalid(phone)) {
//            return Result.fail("手机号格式错误！");
//        }
//
//        // 校验验证码
//        String code = loginForm.getCode();
//        Object cacheCode = session.getAttribute("code");
//        // 如果验证码为空或者不相等
//        if (cacheCode == null || !code.equals(cacheCode)) {
//            return Result.fail("验证码错误！");
//        }
        // Redis 版本
        // 校验手机号格式
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 从Redis数据库获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !code.equals(cacheCode)) {
            return Result.fail("验证码错误！");
        }
        // 从数据库查询手机号相符的user
        User user = query().eq("phone", phone).one();
        // 如果不存在则创建
        if (user == null) {
            // 新用户创建
            log.info("创建新用户");
            user = createUserWithPhone(phone);
        }
//        session.setAttribute("user", UserHolder.transerToDTO(user));
        // 保存用户信息到Redis，同时生成token
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = UserHolder.transerToDTO(user);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((filedName, filedValue) -> filedValue.toString()));
        // 存储用户信息到Redis
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 返回token
        return Result.ok(token);

    }

    @Override
    public Result logout() {
        UserHolder.removeUser();
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        // 新用户创建只需要手机号和初始化的昵称
        User user = new User();
        user.setPhone(phone);
        // 初始化用户的昵称
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
