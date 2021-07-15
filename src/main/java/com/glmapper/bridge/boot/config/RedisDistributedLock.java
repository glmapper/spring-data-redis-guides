/*
 * Copyright (c) 2014-2017 Huami, Inc. All Rights Reserved.
 */

package com.glmapper.bridge.boot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName RedisDistributedLock
 * @Description Distributed locking based on Redis implementation
 * @Author songguolei
 * @Date 2021/7/8 18:47
 * @Version 1.0
 */
@Configuration
public class RedisDistributedLock {

    /**
     * Flag of Lock success
     */
    private static final Boolean LOCK_SUCCESS = true;

    /**
     * 释放锁成功
     */
    private static final Long RELEASE_SUCCESS = 1L;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * lock
     *
     * 1、value maybe use threadId
     * 2、use setIfAbsent to implement set_nx & expire
     *
     * @param key
     * @param value
     * @param expireTime
     * @return
     */
    public boolean tryToLock(String key, String value, int expireTime) {
        // setIfAbsent  has set_nx & expire capabilities as of SpringBoot 2.x
        // set_nx，return true on success, indicating that the lock was successful
        Boolean lockResult = redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.SECONDS);
        boolean result = LOCK_SUCCESS.equals(lockResult);
        if (result) {
            // This provides a lock renewal capability to prevent a thread from executing too long
            // and not finishing execution within the expiration time, causing the lock to be released
            RedisRenewalDaemonThread renewalDaemonThread = new RedisRenewalDaemonThread(redisTemplate, key, expireTime);
            renewalDaemonThread.setDaemon(true);
            renewalDaemonThread.start();
        }
        return result;
    }

    /**
     * release lock, which is done using the Lua script
     *
     * @param key
     * @param value
     * @return
     */
    public boolean releaseLock(String key, String value) {
        String luaScript = "if (redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        List<String> keyList = new ArrayList<>();
        keyList.add(key);
        Long result = redisTemplate.execute(redisScript, keyList, value);
        return RELEASE_SUCCESS.equals(result);
    }
}