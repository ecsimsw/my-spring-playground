package com.ecsimsw.gateway.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash(value = "BlockedUser")
public class BlockedUser implements Serializable {

    @Id
    private String userId;

    public BlockedUser(String userId) {
        this.userId = userId;
    }
}


