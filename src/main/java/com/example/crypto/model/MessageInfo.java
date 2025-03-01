package com.example.crypto.model;

import lombok.Builder;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash("MessageInfo")
@Builder
public class MessageInfo implements Serializable {
    private String id;
    private Message message;
    private long from;
    private long to;

}
