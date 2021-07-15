/*
 * Copyright (c) 2014-2017 Huami, Inc. All Rights Reserved.
 */

package com.glmapper.bridge.boot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @ClassName TestStartupListener
 * @Description TODO
 * @Author songguolei
 * @Date 2021/7/8 17:36
 * @Version 1.0
 */
@Component
public class TestStartupListener implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
        redisTemplate.opsForValue().set("myKey", "myValue",3, TimeUnit.SECONDS);
        String myKey = redisTemplate.opsForValue().get("myKey");
        System.out.println("expired when get from redis: "+ myKey);
    }
}