package com.hmdp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
