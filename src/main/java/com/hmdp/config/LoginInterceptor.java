package com.hmdp.config;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    // 登陆前验证用户是否存在
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求中获取会话
        HttpSession session = request.getSession();
        // 获取用户属性
        UserDTO user = (UserDTO) session.getAttribute("user");
        if (user == null) {
            // 用户不存在则进行拦截，返回401未授权错误码
            log.info("用户不存在");
            response.setStatus(401);
            return false;
        }
        UserHolder.saveUser(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 在请求结束后清除用户信息，避免内存泄漏
        UserHolder.removeUser();
    }
}
