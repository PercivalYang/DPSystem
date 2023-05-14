package com.hmdp;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.impl.UserServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class HmDianPingApplicationTests {
    @Resource
    UserServiceImpl userService;
    @Test
    public void testUserDTO() {
        User user = userService.query().eq("phone", "13686869696").one();
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        System.out.println("user = " + user);
    }
}
